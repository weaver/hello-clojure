;;;; Functional Data and XML
;;;;
;;;; Clojure provides immutable data structures and emphasizes
;;;; functional transformation. This example shows how these data
;;;; structures can be used to change an XML document.

(ns functional-data.test.core
  (:require
   [clojure.xml :as xml]
   [clojure.zip :as zip])
  (:use [functional-data.core])
  (:use [clojure.test]))


;;; Maps

;; There are several immutable data structures provided by
;; Clojure. One of them is a map; it associates keys to values for
;; quick lookup. Since it's an immutable structure, modifiying it
;; produces a new map that shares its structure with the original.

(deftest maps
  (let [orig {:a 1 :b 2}]

    (is (map? orig)
        "Maps have a recognizable type.")

    (is (= (:a orig) 1)
        "Keys can be looked up quickly.")

    (is (= (assoc orig :b 3 :c 4)
           {:a 1 :b 3 :c 4})
        "Associating keys with a map creates a new map.")

    (is (= (dissoc orig :a) {:b 2})
        "Disassociating keys also creates a new map.")

    (is (= orig {:a 1 :b 2})
        "The original map is preserved.")))


;;; XML

;; Clojure's XML parser converts a document into a nested structure of
;; maps and vectors. The built-in `xml/parse` function accepts a
;; filename or stream. One of the convenience functions this project
;; provides is a form called `parse-xml-str` that parses an XML
;; document from a string.

(deftest parsing-xml
  (let [data "<doc a='1'>Hello!</doc>"
        node (parse-xml-str data)]

    (is (= node
           {:tag :doc
            :attrs {:a "1"},
            :content ["Hello!"]})
        "XML nodes are represented as maps with items.")))

;; Since maps and vectors are both associative structures, the
;; `assoc-in` function is useful for modifying nodes. It takes a map
;; or vector, a path of keys or indicies, and a new value to place at
;; that location. A new object is returned, sharing its structure with
;; the original.

(deftest transforming-xml
  (let [data "<doc a='1'>Hello!</doc>"
        node (parse-xml-str data)]

    (is (= (assoc-in node [:content 0] "Goodbye!")
           {:tag :doc
            :attrs {:a "1"}
            :content ["Goodbye!"]})
        "The `assoc-in` method is convenient for transforming nodes.")))

;; To convert a node back into a serialized representation, use
;; `xml/emit` and `xml/emit-element`. These methods print out the XML
;; document; use `with-out-str` to capture it as a string.

(deftest serializing-xml
  (let [node (parse-xml-str "<doc a='1' />")]

    (is (= (with-out-str (xml/emit-element node))
           "<doc a='1'/>\n")
        "A node can be serialized back to a string with `xml/emit-element`.")

    (is (= (with-out-str (xml/emit node))
           "<?xml version='1.0' encoding='UTF-8'?>\n<doc a='1'/>\n")
        "The `xml/emit` function adds an XML declaration.")))


;;; SXML

;; Clojure's XML node representation is straightforward, but a little
;; bit verbose for writing out literally. Since XML and s-expressions
;; are similar structures, it's easy to write [SXML][0]. Part of this
;; project implements functions that convert between SXML and XML-node
;; representations.

;; SXML is written as a quoted form where the "operator" is the tag,
;; optionally followed by a map of attributes, and the rest of the
;; form is the element's body. The dialect used here is different than
;; the spec. Since Clojure natively supports maps, they're used to
;; represent attributes instead of the traditional `(@ (key val) ...)`
;; form.


(deftest sxml
  (let [doc
        `(:doc {:a "1"}
           (:body
            "Hello!"))]

    (is (= (sxml->node doc)
           {:tag :doc
            :attrs {:a "1"}
            :content [{:tag :body, :attrs nil, :content ["Hello!"]}]})
        "A SXML structure can be converted to a node.")

    (is (= (node->sxml (sxml->node doc)) doc)
        "Conversion between node and SXML representations is idempotent.")

    (is (= (with-out-str (sxml-emit-element doc))
           "<doc a='1'>\n<body>\nHello!\n</body>\n</doc>\n")
        "SXML can be serialized as well.")))


;;; Zippers

;; Functionally updating a large, nested data structure like an XML
;; document can be tedious. Since values can't be modified in place,
;; changing a something several levels down from the top means each
;; parent node has to be reconstructed along the way up.

;; A zipper solves this problem by combining the notion of navigation
;; through a function data structure with the ability to edit
;; locations in the structure functionally. Consider this document:

(def example-doc
  `(:html
    (:head)
    (:body
     (:h1 "Example")
     (:ul
      (:li "First")))))

;; To append a new element to the list, something must walk down the
;; document tree to find the `ul`, "unzipping" it along the way. When
;; a new item can be appended, a new `ul` is created. The `body` is
;; then modified to replace old `ul` the new one. This process is
;; repeated as the tree is "zipped" back up to the root.

;; Because zippers allow arbitrary movement through a structure,
;; there's no need to re-zip the entire document repeatedly when
;; multiple edits need to be made. This minimizes the number of nodes
;; created. It also makes editing a document transactional.

(deftest zipper-example
  ;; The `zip/xml-zip` function creates a new zipper over a root node.
  (let [top (sxml-zip example-doc)]

    (is (= (tag top) :html)
        "The current node can be extracted.")

    ;; Moving the cursor down makes a "checkpoint" in the transaction.
    (let [ul (zip/right (zip/down (zip/right (zip/down top))))]

      (is (= (tag ul) :ul)
          "Moving down and right brings the cursor to the UL element.")

      ;; The `ul` node is edited by appending a new `li` node to
      ;; it. This is the only edit being made, so use `sxml-root`
      ;; produce a new top-level node.
      (let [new-ul (zip/append-child ul (sxml->node `(:li "Second")))
            new-doc (sxml-root new-ul)]

        (is (= new-doc
               `(:html
                 (:head)
                 (:body
                  (:h1 "Example")
                  (:ul
                   (:li "First")
                   (:li "Second")))))
            "After editing is done, `sxml-root` produces a new document.")))

    (is (= (node top)
           (sxml->node example-doc))
        "The original zipper remains unmodified.")

    (is (= example-doc
           `(:html
             (:head)
             (:body
              (:h1 "Example")
              (:ul
               (:li "First")))))
        "So does the example document.")))


;;; XML Transformation

;; This project implements higher-level traversal functions and
;; helpers for modifying attributes and content. Together, these make
;; simplify the task of transforming an XML document.

;; A straightforward traversal strategy is to describe the movement in
;; terms of a path. For example, `[:html :head :body :h1]` identifies
;; the location of the `:h1` element in `example-doc`. Traversal
;; functions recognize when to stop moving using by matching each path
;; segment.

(deftest segment-matching
  (let [top (sxml-zip example-doc)]

    (is (segment-matches? :html top)
        "Does this segment expression match the current location?")

    (is ((match-segment :html) top)
        "Close over a segment expression to create a location
        predicate.")))

;; Moving is the counterpart to matching in a traversal strategy. The
;; built-in zipper provides primitive movement commands. These
;; higher-level functions can step through multiple locations until a
;; condition is satisfied.

(deftest conditional-movement
  (let [top (sxml-zip example-doc)]

    (is (= (tag
            (move-until zip/down top (match-segment :head)))
           :head)
        "Continue stepping in the given direction until a predicate is
        satisfied.")

    (is (= (tag
            (move-to zip/down top :head))
           :head)
        "A terser variant of `move-until` that takes a segment instead
        of a predicate.")))

;; Depth-first scanning is straightforward with these movement
;; commands. The `forward` and `backward` methods close over a path,
;; creating traversal methods that can be applied to locations.

(deftest walking
  (let [top  (sxml-zip example-doc)
        h1   ((forward :html :body :h1) top)
        li   ((forward :ul :li) h1)
        head ((backward :li :ul :head) li)]

    (testing "Walking around the tree."
      (is (= (tag h1) :h1))
      (is (= (tag li) :li))
      (is (= (tag head) :head)))))

;; Editing commands make changes to a particular node after walking to
;; it.

(deftest editing
  (let [top (sxml-zip `(:doc))]

    (is (nil? (attrs top))
        "The attributes are initially empty.")

    (is (= (attrs
            (edit-attrs top :foo "bar"))
           {:foo "bar"})
        "The `set-attrs` method uses `zip/edit` and `attr-setter`.")

    (is (nil? (content top))
        "The content is initially empty.")

    (is (= (content
            (edit-content top "Hello!"))
           ["Hello!"])
        "The `set-content` method uses `zip/edit` and
        `content-setter`.")

    (is (= (sxml-root ((modify {:a "alpha"} "Goodbye!") top))
           `(:doc {:a "alpha"}
                  "Goodbye!"))
        "Setters can be composed with modify.")))

;; These traversal and editing methods can be combined to make several
;; transformations to a document at once.

(deftest complete-example
  (is (= (transform-sxml example-doc
          (forward :html :body :ul)
          (append `(:li "Second"))
          (backward :h1)
          (modify {:changed "true"} "Example (Updated)")))
      `(:html
        (:head)
        (:body
         (:h1 {:changed "true"} "Example (Updated)")
         (:ul
          (:li "First")
          (:li "Second"))))))

;; [0]: http://okmij.org/ftp/Scheme/SXML.html
