;;;; Basic IO Tests
;;;;
;;;; Here are some unit tests. There's nothing new to see here,
;;;; they're just for practice.

(ns basic-io.test.core
  (:use [basic-io.core])
  (:use [clojure.test]))

(deftest test-markdown
  (is (= (with-out-str
           (source-to-markdown
            ";;;; Title\n;;;;\n;;;; Hello, world\n\formfeed\n;;; Main\n\n(do-it)"))
         "# Title #\n\nHello, world\n\formfeed\n## Main ##\n\n    (do-it)\n")))


(deftest test-repeat-string
  (is (= "" (repeat-string "a" 0)))
  (is (= "a" (repeat-string "a" 1)))
  (is (= "aaaaa" (repeat-string "a" 5))))

(deftest test-starts-with
  (is (starts-with "foo" \f))
  (is (not (starts-with "foo" \o))))

(deftest test-with-in-file
  (is (= (with-in-file "test/basic_io/test/core.clj"
           (read-line))
         ";;;; Basic IO Tests")))

(deftest test-next-line
  (is (nil? (catch-eof (with-in-str "" (next-line)))))
  (is (= (catch-eof (with-in-str "foo" (next-line))) "foo")))

(deftest test-split-semi
  (is (= [";;" "foo"] (split-semi ";; foo"))))

