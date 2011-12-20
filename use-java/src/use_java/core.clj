;;;; Using Java from Clojure
;;;;
;;;; This example shows how to convert Markdown to HTML with the
;;;; native Java project [pegdown][0] from Clojure. Begin by adding
;;;; the dependency to `project.clj` and an `:import` to the `ns`
;;;; declaration of a Clojure module.

(ns use-java.core
  (:import [org.pegdown PegDownProcessor Extensions]))


;;; PegDown

;; Static properties can be accessed using either `CLASS/NAME` or
;; `(. CLASS NAME)`. Instances are constructed with a
;; `(CLASS. arg...)` form. Methods are invoked by passing the instance
;; and arguments to a method: `(.methodName obj arg...)`. See [Java
;; Interop][1] for more details.

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