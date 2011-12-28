;;;; Hello World Tests
;;;;
;;;; Clojure comes with a nice unit testing framework built in. Tests
;;;; can be run using `lein test`. See "Writing the Code" in the
;;;; [Leiningen Tutorial][1] for more details.
;;;;
;;;; Unit tests can also be run from the REPL. Go into a test file
;;;; like this one and load it (e.g. `C-c C-l`). Then do a
;;;; `(run-tests)` at the REPL. The `run-tests` form optionally
;;;; accepts names of specific test suites to run.

(ns hello-world.test.core
  (:use [hello-world.core])
  (:use [clojure.test]))


;;; Tests

;; The [`clojure.test`][0] package exports `deftest` to define tests
;; and `is` to make assertions.

(deftest verify-main
  (is (= (with-out-str (-main))
         "Hello, world.\n")
      "The main program produces the expected output."))

;; [0] http://clojure.github.com/clojure/clojure.test-api.html
;; [1] https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md
