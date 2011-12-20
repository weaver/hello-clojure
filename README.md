# Hello Clojure! #

These are a series of small projects written while learning
Clojure. It assumes some knowledge of Emacs and Lisp, but no
experience with Clojure or Java.

+ hello-world
+ basic-io


## Installation ##

Follow these steps to set up Clojure with Emacs integration on OS X:

  + Install Java Developer for Mac OS X (via Apple Developer Portal)

  + Set up [package.el and Marmalade][6]

  + Install [clojure-mode][7] and [paredit][8]

  + Install [leinengen][9]

  + `lein plugin install swank-clojure 1.3.3`

  + `lein new hello-world`

  + Open `hello-world/project.clj` in Emacs

  + `M-x clojure-jack-in` to start SWANK.

Changes to `.emacs` include:

    ;; package.el and Marmalade
    (require 'package)
    (add-to-list
      'package-archives
      '("marmalade" . "http://marmalade-repo.org/packages/"))
    (package-initialize)

    ;; paredit
    (autoload 'paredit-mode "paredit"
      "Minor mode for pseudo-structurally editing Lisp code."
      t)

    ;; clojure-mode
    (defun customize-clojure-mode ()
      (paredit-mode 1)
      (local-set-key "M-{" 'paredit-wrap-curly)
      (local-set-key "M-}" 'paredit-close-curly-and-newline)
      (local-set-key "M-[" 'paredit-wrap-square)
      (local-set-key "M-]" 'paredit-close-square-and-newline))

    (add-hook 'clojure-mode-hook 'customize-clojure-mode)


## SLIME ##

Some keyboard shortcuts:

  + **C-x C-e**: Evaluate the form at point
  + **C-x C-r**: Evaluate the selected region
  + **C-c C-z**: Switch to slime REPL
  + **C-c M-p**: Change current REPL package


## More Documentation ##

Some helpful references are:

+ [Mark Volkmann's Tutorial][0]
+ [Leiningen Tutorial][1]
+ [swank-clojure][5]
+ [Clojure Cheatsheet][2]
+ [Clojure Docs][3]
+ [Java Docs][4]


## Q & A ##

  + What's the difference between `java.*` and `javax.*`? The `java`
    namespace is "core" and `javax` are extensions
    ([Stack Overflow][10]).


[0]: http://java.ociweb.com/mark/clojure/article.html
[1]: https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md
[2]: http://clojure.org/cheatsheet
[3]: http://clojure.github.com/clojure/
[4]: http://docs.oracle.com/javase/7/docs/api/index.html
[5]: https://github.com/technomancy/swank-clojure
[6]: http://marmalade-repo.org/
[7]: http://marmalade-repo.org/packages/clojure-mode
[8]: http://marmalade-repo.org/packages/paredit
[9]: https://github.com/technomancy/leiningen
[10]: http://stackoverflow.com/questions/727844/javax-vs-java-package
