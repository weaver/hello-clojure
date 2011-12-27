;;;; Functional Data and XML Implementation
;;;;
;;;; The goal of this package is to create a higher-level interface
;;;; for editing XML documents with Clojure's built-in zipper
;;;; module.
;;;;
;;;; The general strategy is to provide a `transform-sxml` driver
;;;; function that applies a series of transformations to an
;;;; tree. Each transformation is a function takes a zipper and moves
;;;; it or edits its node.
;;;;
;;;; This package also provides:
;;;;
;;;; + Ability to parse XML string literals.
;;;; + SXML support for more convenient literals.
;;;; + Methods that express common editing tasks.
;;;; + Simple path-based tree traversal for more expressive movement.
;;;;
;;;; For example:
;;;;
;;;;     (let [doc `(:html (:body (:h1 "Title Here")))]
;;;;       (transform-sxml doc
;;;;         (forward :html :body :h1)
;;;;         (modify {:id "title"} "My Document")
;;;;         (backward :body)
;;;;         (append `(:p "Hello, world!"))
;;;;
;;;; produces this new document:
;;;;
;;;;     (:html
;;;;       (:body
;;;;         (:h1 "Title Here")
;;;;         (:p "Hello, world!")))
;;;;
;;;; The SXML representation differs from Scheme's standard. Maps are
;;;; used for attribute lists since Clojure supports them
;;;; natively. Keywords are used for element and attribute names
;;;; because Clojure namespaces symbols. It could be interesting to
;;;; map XML namespaces onto Clojure namespaces and use symbols
;;;; instead.

(ns functional-data.core
  (:require
   [clojure.xml :as xml]
   [clojure.zip :as zip])
  (:import [java.io ByteArrayInputStream]))


;;; Parsing & Construction

;; Helpers for parsing and serializing XML or SXML. The SXML format is
;; more convenient for writing literals in code while Clojure's
;; built-in XML format is better for quick navigation and modification
;; with built-in functions like `assoc-in`.

(defn str->byte-array-input [str]
  (ByteArrayInputStream. (.getBytes str)))

(defn parse-xml-str [str]
  (xml/parse (str->byte-array-input (.trim str))))

(defn sxml->node
  "Convert SXML representation to a parsed-node representation."

  [doc]
  (if (string? doc)
    doc
    (let [[tag maybe-attr & more] doc
          attr (if (map? maybe-attr) maybe-attr nil)
          body (if (map? maybe-attr) more (rest doc))
          content (if (empty? body) nil (vec (map sxml->node body)))]

      {:tag tag
       :attrs attr
       :content content})))

(defn node->sxml
  "Convert a parsed XML node into an SXML representation."

  [node]
  (if (string? node)
    node
    (let [tag (:tag node)
          attr (:attrs node)
          content (:content node)
          body (if (nil? content) '() (map node->sxml content))]
      (if (nil? attr)
        (cons tag body)
        (cons tag (cons attr body))))))

(defn sxml-emit
  [doc]
  (xml/emit (sxml->node doc)))

(defn sxml-emit-element
  [doc]

  (xml/emit-element (sxml->node doc)))


;;; Nodes

;; These methods offer convenient inspection and editing capabilities
;; for nodes. Many have a zipper counterpart. Editing is done by
;; closing over new content and producing a method that can be applied
;; to a node. These can be composed or used with `zip/edit`.

(defn attr
  "Get the value of a node's attribute."

  [node name]
  (name (:attrs node)))

(defn set-attrs [& attrs]
  "Create a node transformer over these attributes."

  (fn [node]
    (assoc node :attrs (apply assoc (:attrs node) attrs))))

(defn set-content
  "Transform a node by setting its content"

  [input]
  (let [content (if (vector? input) input [input])]
    (fn [node]
      (assoc node :content content))))


;;; Zippers

;; Support the creation and finalization of zippers over SXML
;; documents by converting the documents to a node representation
;; first. Methods for extracting information from a location or
;; editing it are also provided.

(defn sxml-zip
  "Create a zipper over an SXML document."
  [doc]

  (zip/xml-zip (sxml->node doc)))

(defn sxml-root
  "Zip up from the given location, creating a new SXML document."
  [loc]

  (node->sxml (zip/root loc)))

(defn node
  "Return the node for the current location, can handle nil location."
  [loc]

  (if (nil? loc)
    nil
    (zip/node loc)))

(defn tag
  "Find the tag for a location."

  [loc]
  (:tag (node loc)))

(defn attrs
  "Return the attributes for a location."

  [loc]
  (:attrs (node loc)))

(defn content
  "Find the content for a location."

  [loc]
  (:content (node loc)))

(defn edit-attrs
  "Add attributes to this location."

  [loc & attrs]
  (zip/edit loc (apply set-attrs attrs)))

(defn edit-content
  "Change the content of this location."

  [loc content]
  (zip/edit loc (set-content content)))

(defn restructurer
  "Change the document structure somehow."

  [method]
  (fn [doc]
    (let [node (sxml->node doc)]
      #(method % node))))

(def append-child (restructurer zip/append-child))

(def insert-child (restructurer zip/insert-child))

(def insert-left  (restructurer zip/insert-left))

(def insert-right (restructurer zip/insert-right))

(def replace      (restructurer zip/replace))

(def remove zip/remove)


;;; Traversal

;; The traversal strategy is a very simple path-based, depth-first
;; approach. Richer path expressions could be supported by compiling
;; each segment into a predicate and direction/axis like XPath.

(defn segment-matches?
  "Does the zipper `loc` match this path segment?"

  [seg loc]
  (= (:tag (zip/node loc)) seg))

(defn match-segment
  "Create a predicate that tests whether a node matches the given path
  segment."

  [seg]
  (partial segment-matches? seg))

(defn move-until
  "Move in a particular direction until the current node matches a
  predicate."

  [dir zipper match?]
  (loop [loc zipper]
    (if (or (nil? loc) (match? loc))
      loc
      (recur (dir loc)))))

(defn move-to
  "Move in a particular direction until the current node matches a
  segment."

  [dir zipper segment]
  (move-until dir zipper (match-segment segment)))

(defn walk-to
  "Traverse a zipper depth-first, following `full-path` laterally and
  vertically to a target location. The path must be exact; that is, a
  node must be named at each level of the tree from the origin to the
  destination."

  [sideways vertical zipper full-path]
  (if (or (empty? full-path) (nil? zipper))
    ;; The path is empty, which means `loc` is the target or `loc` is
    ;; empty, which means nothing was found. In either case, fast-path
    ;; exit the function.
    zipper

    ;; Loop through the path, moving the zipper until a match is found
    ;; for each segment.
    (loop [loc zipper
           [segment & remainder] full-path]
      (let [probe (move-to sideways loc segment)]
        (cond
         ;; No match for this segment, stop.
         (nil? probe)
         nil

         ;; Found a match, if nothing's left on the `path` we're done.
         (empty? remainder)
         probe

         ;; There's still a `path` left, move down into this node.
         true
         (recur (vertical probe) remainder))))))


;;; Editing

;; Zippers are transformed by a series of functions that take a
;; location as input and produce an updated location as output. A
;; transformation could be a movement or an edit.

(defn transformer
  "Reduce a location by applying a series of movements and
  transformations."

  [& transforms]
  (fn [top]
    (loop [loc top
           [edit & more] transforms]
      (if (nil? edit)
        loc
        (recur (edit loc) more)))))

(defn transform-sxml
  "Apply a series of transformations to an SXML document, producing a
  new document."

  [doc & transforms]
  (let [transform (apply transformer transforms)]
    (sxml-root
     (transform
      (sxml-zip doc)))))


;; Editing transformations are expressed with the `modify`
;; method. Each argument is "compiled" into a editing method with
;; something like `set-attrs` or `set-content`.

(defn flatten-map
  "Convert a map into a sequence of [k1 v1 k2 v2 ...]."

  [obj]
  (interleave (keys obj) (vals obj)))

(defn compile-modifier
  "Close over a modifier given to `modify` using the appropriate
  editing method."

  [change]
  (if (map? change)
    (apply set-attrs (flatten-map change))
    (set-content change)))

(defn modify
  "Compose a set of modifications into one editing function."

  [& changes]
  (let [change (apply comp (map compile-modifier changes))]
    #(zip/edit % change)))


;; Movement transformations close over an expression that identifies
;; and end-point. The direction is baked in through the function name.

(defn left-to  [segment] #(move-to zip/left % segment))

(defn right-to [segment] #(move-to zip/right % segment))

(defn up-to    [segment] #(move-to zip/up % segment))

(defn down-to  [segment] #(move-to zip/down % segment))

(defn forward  [& path]    #(walk-to zip/right zip/down % path))

(defn backward [& path]    #(walk-to zip/left zip/up % path))
