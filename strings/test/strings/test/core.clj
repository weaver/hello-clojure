;;;; Strings and Regular Expressions
;;;;
;;;; Processing and munging strings is a fundamental task in many
;;;; programs. Clojure offers strings and regular expressions at its
;;;; core. Additional support is available through a supplemental
;;;; `core.string` package or by using native Java classes and
;;;; methods.

(ns strings.test.core
  (:use [strings.core])
  (:use [clojure.test]))


;;; Example Methods

;; The methods implemented in this package demonstrate various aspects
;; of working with strings. Otherwise, there's no particular
;; motivation for them :-)

(deftest test-repeat

  (is (= (repeat-string "ab" 3)
         "ababab")
      "A string can be repeated a certain number of times."))

(deftest test-starts-with?

  (is (starts-with? "apple" \a)
      "A string may start with a single character.")

  (is (not (starts-with? "apple" \b))
      "But `false` is returned when it doesn't.")

  (is (starts-with? "apple" "ap")
      "A string may start with a sub-string.")

  (is (not (starts-with? "apple" "pp"))
      "An it may not.")

  (is (starts-with? "apple" "apple")
      "A string starts with itself.")

  (is (not (starts-with? "apple" "applejack"))
      "But it doesn't start with something longer than itself."))

(deftest test-split-semicolons

  (is (= (split-semicolons ";; hello")
         '(";;" "hello"))
      "When a string begins with one or more semicolons, they're split
      off.")

  (is (= (split-semicolons "just a line")
         '(nil nil))
      "When no semicolons are found, `nil` is returned."))

(deftest test-strip

  (is (= (strip "mumble quux" "mux")
         "ble q")
      "Characters from the given set are removed from the beginning
      and end of the string.")

  (is (= (strip "foo bar" "baz")
         "foo bar")
      "When no characters match, the original string is returned."))

(deftest test-join-paths

  (is (= (join-paths "foo" "bar")
         "foo/bar")
      "Paths are joined with a forward slash.")

  (is (= (join-paths "a/" "/b/" "c")
         "a/b/c")
      "Leading and trailing slashes on segments are normalized."))
