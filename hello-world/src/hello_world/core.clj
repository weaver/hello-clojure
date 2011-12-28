;;;; Hello World, a very simple Clojure example
;;;;
;;;; This program just prints "Hello, world." when it's run. It
;;;; demonstrates how create a new Clojure project with [Lein][1] and
;;;; add a main entry point.
;;;;
;;;; + Use `lein new NAME` to create a new project.
;;;; + Add a `:main NAME.core` entry to the `defproject` in `project.clj`.
;;;; + Update `core.clj` with your main program.
;;;; + A project can be run using `lein run`.
;;;; + Create a standalone `.jar` using `lein uberjar`.
;;;;
;;;; Once all of this is done, run the jar from the command line like
;;;; this:
;;;;
;;;;     java -jar hello-world-0.1.0-standalone.jar
;;;;


;;; Main Program

;; Add a `:gen-class` form to the `ns` declaration and make a `-main`
;; function. Why does this work?
;;
;; + The `(:gen-class)` option to `ns` uses [`gen-class`][2] to create
;;   a compiled bytecode class and writes a corresponding `.class`
;;   file.
;;
;; + The main procedure is called `-main` because `gen-class` uses `-`
;;   as the prefix for class method names by default ([see also][3]).
;;
;; + Clojure supports variable-arity functions using the `& args`
;;   syntax at the end of a parameter list.
;;
;; + The `:main` declaration in the `defproject` tells Leiningen where
;;   to find the main entry point.

(ns hello-world.core
  (:gen-class))

(defn -main [& args]
  (println "Hello, world."))

;; [1]: https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md
;; [2]: http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/gen-class
;; [3]: http://stackoverflow.com/questions/5305313/what-does-the-idiomatic-defn-main-mean-in-a-clojure-program


