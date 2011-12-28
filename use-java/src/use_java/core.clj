;;;; Using Java from Clojure
;;;;
;;;; The use of [pegdown][0] demonstrates how Java libraries can be
;;;; referenced directly from Clojure programs. Functions declared
;;;; below make the pegdown dependency transparent to consumers of
;;;; this project. For example, to read in markdown from a file and
;;;; print the corresponding HTML:
;;;;
;;;;     (file-to-html "path/to/file.md")
;;;;
;;;; A dependency has been added to `project.clj` and an `:import` to
;;;; the `ns` declaration of this module.

(ns use-java.core
  (:import [org.pegdown PegDownProcessor Extensions]))


;;; PegDown

;; Clojure's dot special form is the basis for interacting with
;; Java. There are also several shortcut forms for common cases. For
;; example:
;;
;; + Static properties can be accessed using `(. CLASS NAME)`
;;   or `CLASS/NAME`
;;
;; + Instances are constructed with a `(CLASS. arg...)` form or the
;;   new macro: `(new CLASS arg...)`
;;
;; + Methods are invoked by passing the instance and arguments to a
;;   method: `(.methodName obj arg...)` or with the dot form: `(. obj
;;   (methodName arg...))`
;;
;; See [Java Interop][1] for more details.
;;
;; Pegdown works by creating a `PegDownProcessor` instance and calling
;; its `markdownToHtml` method on a string or stream. A processor can
;; accept optional flags to modify the parser's behavior.

(def DEFAULT-OPTIONS
  (bit-or
   Extensions/SMARTYPANTS
   Extensions/AUTOLINKS
   Extensions/DEFINITIONS))

(defn markdown-to-html
  "Convert markdown source to HTML."

  [body]
  (let [processor (PegDownProcessor. DEFAULT-OPTIONS)]
    (.markdownToHtml processor body)))

;; The built-in `slurp` method reads an entire file into a String.

(defn file-to-html
  "Convert a markdown file to HTML."

  [path]
  (markdown-to-html (slurp path)))

;; [0]: https://github.com/sirthias/pegdown
;; [1]: http://clojure.org/java_interop
