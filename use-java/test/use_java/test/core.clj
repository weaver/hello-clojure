;;;; Using Java
;;;;
;;;; This project offers supports the conversion of Markdown documents
;;;; to HTML using the native Java project [pegdown][0]. A document
;;;; may be a file or a string in memory.

(ns use-java.test.core
  (:use [use-java.core])
  (:use [clojure.test]))

;; Example markdown documents and expected HTML output are kept in the
;; adjacent "resources" folder.

(defn resource
  "Expand a resource name to a project-relative path."

  [name]
  (str "test/use_java/resources/" name))

(deftest test-markdown-to-html
  (is (= (markdown-to-html "# foo #\nbar")
         "<h1>foo</h1><p>bar</p>")))

(deftest test-file-to-html
  (is (= (file-to-html (resource "example.md"))
         (slurp (resource "example.html")))))

;; [0]: https://github.com/sirthias/pegdown
