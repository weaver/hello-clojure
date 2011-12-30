;;;; Types in Clojure
;;;;
;;;; Clojure provides a [deftype][0] form for declaring native JVM
;;;; types. A type may have several fields and conform to various
;;;; [protocols][1] that specify abstract interfaces.
;;;;
;;;; Clojure strongly enforces the type/protocol relationship. Methods
;;;; can't be added to a type unless they're declared in a protocol
;;;; first. The benefit of emphasizing the use of protocols is that
;;;; any type can participate in any interface (even after it's
;;;; already been declared with [extend-type][2] or
;;;; [extend-protocol][3]). Using protocols also has the benefit of
;;;; producing nicer-looking code. Methods called directly on a type
;;;; instance must use the dot-operator, but calling through the
;;;; protocol's method hides the use of dot.

(ns types.test.core
  (:use [types.core])
  (:use [clojure.test])
  (:import [java.lang]))


;;; Records

;; A record is a type with some pre-canned abilities like a convenient
;; REPL representation, persistent map interface, and it's
;; immutable. It's best to use a [defrecord][0] for application data
;; and `deftype` for implementation details.
;;
;; A Person record is declared here for use in an example application
;; below. Records can implement protocols or Java interfaces like
;; Comparable.

(defrecord Person [name gender]
  java.lang.Comparable
  (compareTo [this other]
    (or
     (compare (:name this) (:name other))
     (compare (:gender this) (:gender other)))))

(deftest compare-people
  (let [george (Person. "George" :male)
        betty (Person. "Betty" :female)]

    (is (= (:gender george)
           :male)
        "The value of a record's field can be accessed using a keyword
        just like a map.")

    (is (= (sort (list george betty))
           (list betty george))
        "People can be ordered because Person implements
        java.lang.Comparable")))


;;; Example Application

;; This example application uses the `Tree` `deftype` to index people
;; by their name. A record is used for Person since it's application
;; data, but how people are stored is an implementation detail, so
;; `Tree` is a `deftype`. Since `Tree` conforms to the IPersistentMap
;; interface, familiar functions like `assoc` and `vals` can be used
;; by the application code.

(defn open-db
  []
  (tree))

(defn add-people
  [db & people]
  (apply assoc db (interleave (map :name people) people)))

(defn list-people
  [db]
  (vals db))

(defn find-person
  [db name]
  (get db name))

(defn gender-of
  [db name]
  (:gender (find-person db name)))

(deftest example-application
  (let [db (add-people
            (open-db)
            (Person. "George" :male)
            (Person. "Betty" :female)
            (Person. "Alfred" :male)
            (Person. "Taylor" :unknown))]

    (is (= (count db) 4)
        "The database is countable because Tree implements IPersistentCollection")

    (is (= (map :name (list-people db))
           '("Alfred" "Betty" "George" "Taylor"))
        "People are indexed alphabetically by name.")

    (is (= (gender-of db "Taylor")
           :unknown)
        "Respect people's privacy.")))


;;; Tree

;; A Tree conforms to its own special ITree interface as well as
;; familiar interfaces like IPersistentMap and Associative. See
;; `core.clj` for implementation notes. This interface exists to
;; demonstrate how protocols can be declared and implemented. It's
;; very low-level and shouldn't be used directly. In a real program,
;; it may be better to use simple helper functions instead.

(deftest test-itree
  (is (empty-tree? (tree))
      "The nil tree is empty.")

  (is (not (empty-tree? (tree :a 1)))
      "Trees with content are not empty.")

  (let [example (tree :b 1 :a 2 :c)]

    (is (identical? example (replace-left example (.left example)))
        "A new tree isn't produced by replace-left unless it's necessary.")

    (is (= (keys (replace-left example (tree :d 4)))
           (list :d :b :c))
        "Tree invariants can be violated by using replace-left directly.")

    (is (identical? example (replace-right example (.right example)))
        "replace-right also preserves the existing tree when possible.")

    (is (= (keys (replace-right example (tree :a 4)))
           (list :a :b :a))
        "And replace-right can likewise trash the tree when used directly.")

    (is (= (get (change-key example :a #(edit-tree % inc)) :a)
           3)
        "change-key is the fundamental way to modify a tree.")

    (is (= (keys (insert-entry example (make-entry :z 26)))
           (list :a :b :c :z))
        "The `insert-entry` method uses `change-key`.")

    (is (= (keys (remove-key example :a))
           (list :b :c))
        "So does `remove-key`.")))

;; Tree implements IPersistentCollection, ILookup, Associative, and
;; PersistentMap.

(deftest test-persistent-collection
  (let [example (tree :b 1 :a 2 :c 3)]
    (is (= (apply list example)
           '([:a 2] [:b 1] [:c 3]))
        "A Tree implements `seq`, so it can be listed.")

    (is (= (count example)
           3)
        "`count` returns the number of entries.")

    (is (= (apply list (conj example (make-entry :d 4)))
           '([:a 2] [:b 1] [:c 3] [:d 4]))
        "`conj` uses the `cons` implementation to add an entry into the tree.")

    (is (identical? (empty example) nil-tree)
        "`empty` creates a new version without any entries.")

    (is (= example (tree :b 1 :a 2 :c 3))
        "The `equiv` method allows Trees to be compared for equality.")))

(deftest test-lookup
  (let [example (tree :b 1 :a 2 :c 3)]

    (is (= (get example :b) 1)
        "`get` uses `valAt` to return a value.")

    (is (nil? (get example :d))
        "When a key can't be found, `nil` is returned.")

    (is (= (get example :d ::not-found) ::not-found)
        "A third argument can be provided to use the alternate `valAt` form.")))

(deftest test-associative
  (let [example (tree :b 1 :a 2 :c 3)]

    (is (contains? example :b)
        "The `containsKey` method is called by `contains?`.")

    (is (not (contains? example :d))
        "It returns true or false depending on whether a key is in the
        Tree.")

    (is (= (find example :b)
           (make-entry :b 1))
        "`find` uses the `entryAt` method to return an entire entry
        instead of just a value.")

    (is (= (assoc example :d 4 :e 5)
           (tree :a 2 :b 1 :c 3 :d 4 :e 5))
        "`assoc` adds one or more key/val pairs.")))

(deftest test-persistent-map
  (let [example (tree :b 1 :a 2 :c 3)]

    (is (= (dissoc example :a)
           (tree :b 1 :c 3))
        "The `without` method is used by `dissoc` to remove entries.")))

;; [0]: http://clojure.org/datatypes
;; [1]: http://clojure.org/protocols
;; [2]: http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/extend-type
;; [3]: http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/extend-protocol
