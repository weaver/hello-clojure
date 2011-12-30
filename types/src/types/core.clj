;;;; Types in Clojure
;;;;
;;;; This package uses Clojure's [deftype][4] and [protocols][5] to
;;;; implement an unbalanced binary tree. For example:
;;;;
;;;;     (print-tree
;;;;       (dissoc
;;;;         (tree :e 1 :c 2 :a 3 :d 4 :b 5)
;;;;         :c))

(ns types.core
  (:import [clojure.lang MapEntry]))


;;; Basic Tree

;; A tree is composed of entries. Each entry has a key and a
;; value. Use MapEntry for compatibility with Java and Clojure's
;; IPersistentMap protocol.

(defn make-entry
  [key val]
  (MapEntry. key val))

;; The ITree protocol expresses some fundamental operations on
;; trees. Other protocols and higher-level operations can be
;; implemented in terms of these.

(defprotocol ITree
  (empty-tree? [this]
    "Is this tree empty?")

  (find-entry [this key]
    "Return the MapEntry corresponding to `key`.")

  (replace-left [this subtree]
    "Return an updated Tree if `subtree` is different than the current
    left branch.")

  (replace-right [this subtree]
    "Return an updated Tree if `subtree` is different than the current
    right branch.")

  (change-key [this key fn]
    "This is the fundamental change operator. Traverse this tree to
    locate the node corresponding to `key`, calling `replace-*` on the
    way down. When the node is found, apply `fn` to it. The result is
    used as the new node.")

  (insert-entry [this entry]
    "Return an updated tree with `entry` in it.")

  (remove-key [this key]
    "Return an updated tree without `key`.")

  (splice-out [this]
    "Return an updated subtree with this node removed."))

;; Most tree methods will need to make a decision based on a target
;; key and the entry of the current tree node. This macro expresses
;; the decision in terms of a four-way branch of empty, found, go
;; left, and go right.

(defmacro with-branch
  [[target-key entry] empty found left right]
  `(let [target# ~target-key
         entry# ~entry]
     (if (nil? entry#)
       ~empty
       (let [cmp# (compare target# (.key entry#))]
         (cond
          (zero? cmp#) ~found
          (neg? cmp#)  ~left
          true         ~right)))))

;; The Tree type itself has an entry, a right branch for entries with
;; keys less than the current entry and a right branch for entries
;; with keys greater than the current entry. It implements ITree and
;; the ITree methods are in turn used to implement built-in Clojure
;; interfaces
;;
;; + IPersistentCollection
;; + ILookup
;; + Associative
;; + IPersistentMap
;;
;; There's no good documentation on how to implement these interfaces,
;; what the method signatures are, and how they interact with
;; Clojure's core library. Here's some advice:
;;
;; + Look at the [interface declarations][0] in the Clojure source to
;;   find method signatures. Note that interfaces declared in Java
;;   have an implicit "this" parameter that must be added explicitly
;;   by implementors.
;;
;; + Look at how the methods are called in the [Run Time][1] source.
;;
;; + Look at the [clojure.core][2] source. Most of the built-in
;;   interfaces are called via a core function. The core function will
;;   usually just be a light wrapper around its `clojure.lang.RT`
;;   counterpart.
;;
;; + Remember that core function names don't always match interface
;;   method names (e.g. `dissoc` vs `without`, `find` vs `assocEx`).
;;
;; + Some protocols are declared in Clojure, for example
;;   [clojure.core.protocols][3].
;;
;; + Macroexpand a `defrecord` form. Since it implements many of the
;;   built-in interfaces, there are clues to suggest how certain
;;   interfaces should behave.

(declare nil-tree)

(deftype Tree [entry left right]

  ITree
  (empty-tree? [this]
    (nil? entry))

  (find-entry [this key]
    (with-branch (key entry)
      nil
      entry
      (find-entry left key)
      (find-entry right key)))

  (replace-left [this subtree]
    (if (identical? subtree left)
      this
      (Tree. entry subtree right)))

  (replace-right [this subtree]
    (if (identical? subtree right)
      this
      (Tree. entry left subtree)))

  (change-key [this key fn]
    (with-branch (key entry)
      (fn nil)
      (fn this)
      (replace-left this (change-key left key fn))
      (replace-right this (change-key right key fn))))

  (insert-entry [this new-entry]
    (change-key
     this
     (key new-entry)
     (fn [probe]
       (if (nil? probe)
         (Tree. new-entry nil nil)
         (Tree. new-entry (.left probe) (.right probe))))))

  (remove-key [this key]
    (change-key
     this
     key
     (fn [probe]
       (if (nil? probe)
         probe
         (splice-out probe)))))

  (splice-out [this]
    (let [le (empty-tree? left)
          re (empty-tree? right)
          both (and le re)]
      (cond
       both nil
       le   right
       re   left
       true (change-key
             left
             (key (.entry right))
             (fn [_] right)))))

  clojure.lang.IPersistentCollection
  (seq [this]
    (if (empty-tree? this)
      nil
      (lazy-cat (seq left) [entry] (seq right))))

  (count [this]
    (count (seq this)))

  (cons [this new-entry]
    (insert-entry this new-entry))

  (empty [this]
    nil-tree)

  (equiv [this other]
    (boolean
     (or
      (identical? this other)
      (when (identical? (class this) (class other))
        (= (seq this) (seq other))))))

  clojure.lang.ILookup
  (valAt [this key]
    (.valAt this key nil))

  (valAt [this key default]
    (let [probe (find-entry this key)]
      (if (nil? probe)
        default
        (val probe))))

  clojure.lang.Associative
  (containsKey [this key]
    (not (nil? (find-entry this key))))

  (entryAt [this key]
    (find-entry this key))

  (assoc [this key val]
    (insert-entry this (make-entry key val)))

  clojure.lang.IPersistentMap
  (assocEx [this key val]
    (change-key
     this
     key
     (fn [probe]
       (if (nil? probe)
         (Tree. (make-entry key val) nil nil)
         (throw (RuntimeException. "key already exists"))))))

  (without [this key]
    (remove-key this key)))


;;; The Empty Sentinal

;; The value `nil` is used as a sentinal for left and right children
;; of a tree node. Implementing the ITree protocol for it simplifies
;; the Tree type because its methods can recursively call protocol
;; methods without explicitly checks for `nil`.

(extend-protocol ITree
  nil
  (empty-tree? [this]
    true)

  (find-entry [this key]
    nil)

  (replace-left [this subtree]
    nil)

  (replace-right [this subtree]
    nil)

  (change-key [this key fn]
    (fn nil))

  (insert-entry [this entry]
    (Tree. entry nil nil))

  (remove-key [this key]
    this)

  (splice-out [this]
    nil))


;;; Additional Tree Methods

(def nil-tree (Tree. nil nil nil))

(defn tree
  "Construct a tree from a sequence of k1 v1 k2 v2... items."

  [& keys-vals]
  (loop [tree nil-tree
         items keys-vals]
    (if (nil? items)
      tree
      (let [[key val & more] items]
        (recur
         (insert-entry tree (make-entry key val))
         more)))))

(defn edit-tree
  "Change the value of a node by applying an editing function to it."

  [node editor]
  (if (nil? node)
    nil
    (let [entry (.entry node)
          old-val (val entry)
          new-val (editor old-val)]
      (if (identical? new-val old-val)
        node
        (Tree.
         (make-entry (key entry) new-val)
         (.left node)
         (.right node))))))

(defn print-tree
  ([tree]
     (print "\n# Begin Tree #")
     (print-tree tree "")
     (print "# End Tree #"))
  ([tree indent]
     (if (empty? tree)
       (println indent nil)
       (let [deeper (str "  " indent)]
         (print-tree (.left tree) deeper)
         (println indent (.entry tree))
         (print-tree (.right tree) deeper)))))

;; [0]: https://github.com/clojure/clojure/tree/master/src/jvm/clojure/lang
;; [1]: https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
;; [2]: https://github.com/clojure/clojure/blob/master/src/clj/clojure/core.clj
;; [3]: https://github.com/clojure/clojure/blob/master/src/clj/clojure/core/protocols.clj
;; [4]: http://clojure.org/datatypes
;; [5]: http://clojure.org/protocols