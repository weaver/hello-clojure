;;;; Strings and Regular Expressions
;;;;
;;;; Clojure offers basic string and regexp support as part of its
;;;; core library. Additional string methods are available in
;;;; `clojure.string`.
;;;;
;;;; This project demonstrates how to interact with strings by
;;;; implementing various string utility methods.

(ns strings.core
  (:require [clojure.string :as string])
  (:import [java.util.regex Pattern]))


;;; Basic String Support

;; Using `with-out-str` is a simple way to accumulate a string
;; iteratively. It binds `*out*` for the scope of its body, directing
;; print output into a string buffer. The value of the buffer is
;; returned.

(defn repeat-string
  "Create a string by repeatedly appending repeating `str` `limit`
  times."

  [str limit]
  (with-out-str
    (dotimes [_ limit]
      (print str))))

;; Clojure allows strings to be treated as sequences of characters.

(defn starts-with?
  "Does `string` begin with `prefix`?"

  [string prefix]
  (if (char? prefix)
    (= (first string) prefix)
    (loop [[p & rest-p] prefix
           [s & rest-s] string]
      (cond
       (nil? p)   true
       (not= p s) false
       true       (recur rest-p rest-s)))))


;;; Regular Expressions

;; Regular Expressions are straightforward to use. The `#"..."` reader
;; syntax produces a compiled pattern that can be matched using
;; `re-matches`. The result of `re-matches` is a sequence of captured
;; groups. The first item in the sequence is the matched string.
;;
;; See [Pattern][0] in the Java Docs for an overview of pattern
;; syntax.

(defn split-semicolons
  "Split semi-colons that prefix a line off from the rest of the
  line's body. Return (semi rest) values."

  [line]
  (let [[_ semi rest] (re-matches #"^(;+)\s*(.*)\s*$" line)]
    (list semi rest)))

;; Patterns can be compiled dynamically as well. Note the use of
;; `Pattern/quote` to escape any special regex characters in the
;; input.

(defn strips
  "Create a stripping function that removes leading and trailing
  characters in the remove set from a string."

  [remove]

  (let [char-set (str "[" (Pattern/quote remove) "]*")
        pattern (re-pattern (str \^ char-set "(.*?)" char-set \$))]

    (fn [string]
      (let [[_ middle] (re-matches pattern string)]
        middle))))

(defn strip
  "Strip leading and trailing characters in the `remove` set from `string`."

  [string remove]
  ((strips remove) string))


;;; String API

;; Clojure provides additional string methods though the
;; [clojure.string][1] module. For example, the core method `str`
;; concatenates a fixed number of strings, but `string/join` operates
;; on a sequence of strings.

(defn join-paths
  "Forcibly join filesystem path segments."

  [& paths]

  (string/join "/" (map (strips "/") paths)))

;; [0]: http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
;; [1]: http://clojure.github.com/clojure/clojure.string-api.html
