;;;; Tests for Using Java
;;;;
;;;; Here are a few simple tests. Because the library is just a small
;;;; wrapper around pegdown, there's not much that needs to be
;;;; covered.

(ns use-java.test.core
  (:use [use-java.core])
  (:use [clojure.test]))

(deftest test-markdown-to-html
  (is (= (markdown-to-html "# foo #\nbar")
         "<h1>foo</h1><p>bar</p>")))

(deftest test-file-to-html
  (is (= (file-to-html "test/use_java/resources/example.md")
         (slurp "test/use_java/resources/example.html"))))

(run-tests)
