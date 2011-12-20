;;;; Basic I/O with Clojure
;;;;
;;;; This example converts a Clojure source file into a markdown
;;;; document by turning comment blocks into paragraphs and code into
;;;; code-blocks. Headers are declared by placing a page break on the
;;;; line before a comment. Using these conventions, code can be
;;;; written in a semi-literate style and converted to a more
;;;; convenient representation for display.
;;;;
;;;; The overall I/O strategy used in this example is to rebind
;;;; Clojure's `*in*` variable to a stream of source code. The core
;;;; `read-line` function is used to process one line at a time. As
;;;; each line is converted, a corresponding markdown line is printed
;;;; to `*out*` using `print` or `println`. This is a flexible
;;;; approach because `*out*` can be rebound by calling code to
;;;; redirect the result somewhere besides standard output.


;;; Namespace Declaration

;; The `:require` form loads a package with the optional `:as` to
;; create a short alias for it. The `:use` declaration imports
;; bindings directly from a package. See [Clojure Libs and
;; Namespaces][0] for more details.

;; Clojure only supports Java exceptions out of the box. The
;; [slingshot][1] library provides a way to create and catch
;; exceptions/conditions without the need to compile separate Java
;; exception classes. It's included by the `[slingshot]` entry in
;; project.clj's `:dependencies` entry and can be installed using
;; `lein deps`.

(ns basic-io.core
  (:require [clojure.java.io :as jio])
  (:use [slingshot.slingshot :only (throw+ try+)])
  (:gen-class))


;;; Helpers

;; These utilities support functions defined below. Clojure's
;; `with-out-str` captures anything printed and writes it into a
;; StringBuffer. The `dotimes` form executes its loop body a
;; deterministic number of times.

(defn repeat-string
  "Create a string repeatedly appending repeating `str` `limit`
  times."

  [str limit]
  (with-out-str
    (dotimes [_ limit]
      (print str))))

(defn starts-with
  "Does `str` begin with `prefix`?"

  [str prefix]
  (= (first str) prefix))

;; This macro is similar to the builtin `with-in-str` special
;; form. The `jio/reader` method can coerce its argument to a
;; `java.io.Reader` and `binding` changes the value of `*in*` for the
;; duration of its body. The macro template begins with a
;; backtick. Appending `#` to an identifier in a macro body creates a
;; unique name for it for the duration of the template. This prevents
;; it from shadowing something in `body`. The `~` operator unquotes a
;; value and `~@` splices a list into the expanded form.

(defmacro with-in-file
  [path & body]
  `(with-open [stream# (jio/reader ~path)]
     (binding [*in* stream#]
       ~@body)))


;;; Driver Loop

;; Clojure code is converted to a markdown format by parsing the
;; source file line by line. This driver implements the loop. The
;; grammar declared in the next section defines state transition
;; functions.

;; The `next-line` procedure uses slingshot's `throw+` form to raise
;; an EOF exception. The double-colon in `::eof` resolves it in the
;; current namespace (e.g. :basic-io.core/eof). Curly braces create a
;; map (e.g. hash-map).

(defn next-line
  "Read a single line, raising an EOF when the input stream is
  exhausted."

  []
  (let [line (read-line)]
    (if (nil? line)
      (throw+ {:type ::eof})
      line)))

;; Since `next-line` raises an EOF to signal the end of a file, the
;; driver needs to be able to catch this and handle it transparently
;; to the rest of the program. A macro is used here so the driver
;; logic can be written normally without needing a helper function.

(defmacro catch-eof
  "This counterpart to `next-line` catches an EOF that may be raised
  by its body."

  [& body]
  `(try+
    ~@body
    (catch [:type ::eof] {}
      nil)))

;; `deftype` concisely declares a new Java class to track the input
;; state. The grammar transitions from state to state, optionally
;; advancing to the next line.

(deftype Input [line state])

(defn apply-state
  "Apply the state transition function to an Input's line."

  [input]
  ((.state input) input))

(defn advance
  "Create a new Input state by advancing to the next line."

  [input state]
  (Input. (next-line) state))

(defn transition
  "Transition to a Input different state, keeping the same line."

  [input state]
  (Input. (.line input) state))

;; The `loop` and `recur` constructs express recursive loops without
;; consuming additional stack space.

(defn document
  "Main driver loop, transitions from state to state."

  [start]
  (catch-eof
   (loop [s (Input. (next-line) start)]
     (recur (apply-state s)))))


;;; Grammar

;; Here's the actual grammar that examines each line of the source
;; file. Each grammar rule returns a new state to the driver loop,
;; allowing something like coroutines.

;; A `declare` creates forward declarations for the functions below so
;; functions defined earlier can call those defined later. The `do`
;; construct executes several forms sequentially from first to
;; last. Helper methods are defined inside rules with nested `defn`
;; forms. The `split-semi` function returns multiple values in a list;
;; these are destructured with `let`.

(declare maybe-header header block split-semi)

(defn maybe-header
  "Check for a form feed to determine if the next line should be
  tagged as a header."

  [input]
  (let [line (.line input)]
    (if (starts-with line \formfeed)
      (do
        (println line)
        (advance input header))
      (transition input block))))

(defn header
  "Emit the current line as a header by tagging it with `#`
  characters. The number of characters depends on how many semicolons
  prefix the title in the source file. Note that markdown and lisp
  comments use opposite quoting levels (for example `;;;;` -> `#`,
  `;;;` -> `##`, &c)"

  [input]

  (defn level [semi]
    (- 5 (count semi)))

  (defn emit [semi title]
    (let [tag (repeat-string "#" (level semi))]
      (println tag title tag)
      (advance input maybe-header)))

  (let [line (.line input)
        [semi rest] (split-semi line)]
    (if (nil? semi)
      (transition input maybe-header)
      (emit semi rest))))

(defn block
  "Convert a comment or code line to markdown. Comment blocks are
  converted to paragraphs and code is indented with four spaces."

  [input]
  (let [line (.line input)
        [semi rest] (split-semi line)]
    (cond
     (not (nil? semi))
     (println rest)

     (not (nil? (re-matches #"^\s*" line)))
     (println "")

     true
      (println "   " line))

    (advance input maybe-header)))

(defn split-semi
  "Split semi-colons that prefix a line off from the rest of the
  line's body. Return (semi rest) values."

  [line]
  (let [[_ semi rest] (re-matches #"^(;+)\s*(.*)\s*$" line)]
    (list semi rest)))


;;; Main program

;; These high-level methods pull the pieces together, taking source
;; code as input and printing out a markdown document.

(defn to-markdown
  "Convert the current input stream from Clojure source to a markdown document."

  []
  (document header))

(defn source-to-markdown
  "Convert a Clojure source string to markdown."

  [source]
  (with-in-str source
    (to-markdown)))

(defn file-to-markdown
  "Convert a Clojure source file to markdown."

  [path]
  (with-in-file path
    (to-markdown)))

(defn -main [path]
  (file-to-markdown path))

;; [0]: http://blog.8thlight.com/colin-jones/2010/12/05/clojure-libs-and-namespaces-require-use-import-and-ns.html
;; [1]: https://github.com/scgilardi/slingshot
