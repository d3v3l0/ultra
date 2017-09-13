(ns ultra.test.diff
  (:require [clojure.string :as s]
            [puget.color.ansi :as ansi]
            [ultra.printer :refer [cprint]]
            [ultra.test.diff.string :as str-diff]))

(defn- indent
  "Replace all newline breaks with whitespace for left-padding."
  {:added "0.1.3"}
  [x padding]
  (let [whitespace (apply str (repeat padding " "))]
    (apply str
           (drop-last
             3
             (s/replace
               x #"\n" (str "\n" whitespace))))))

(defn pretty
  {:added "0.4.2"}
  [s]
  (s/trim (indent (with-out-str (cprint s)) 10)))

(defn print-expected
  "Pretty-prints expected and actual values."
  {:added "0.1.3"}
  [actual expected]
  (let [expected (pretty expected)
        actual (pretty actual)]
    (println "\nexpected:" expected)
    (println "  actual:" actual)))

(defn list-diff
  "Provides helpful info on list and vector tests."
  {:added "0.1.7"}
  [a e]
  (cond
    (and (= (set a) (set e))
         (= (count a) (count e)))
    "lists seem to contain the same items, but with different ordering."
    (and (= (set a) (set e))
         (> (count a) (count e)))
    "some duplicate items in actual are not in expected."
    (and (= (set a) (set e))
         (< (count a) (count e)))
    "some duplicate items in expected are not in actual."
    (> (count a) (count e))
    "actual is longer than expected."
    (< (count a) (count e))
    "expected is longer than actual."
    (= (count a) (count e))
    "expected and actual contain different items."
    :else
    "this shouldn't happen and means there is a bug in Ultra."))

(defmulti prn-diffs
  "Multimethod for printing results when the values from an is equality
  form return false (e.g. `(is (= 1 2))). Pretty-prints differently depending
  on the values involved."
  {:added "0.1.3"}
  (fn [a b actual expected]
    (cond
      (and (string? a) (string? b)) ::diff-strs
      (and (vector? actual) (vector? expected)) ::diff-vecs
      (and (list? actual) (list? expected)) ::diff-vecs
      (and (not= (class a) (class b))
           (some? a)
           (some? b)) ::wrong-class
      :default [a b actual expected])))

(defmethod prn-diffs ::diff-strs
  [a b actual expected]
  (print-expected actual expected)
  (print "\n    diff:")
  (println (str-diff/clean-difform-str a b)))

(defmethod prn-diffs ::wrong-class
  [a b actual expected]
  (print-expected actual expected)
  (let [puget-str (fn [x] (s/trim-newline (with-out-str (cprint x))))]
    (print "\nexpected: ")
    (let [a (with-out-str (println (puget-str b)
                                   "to be an instance of"
                                   (class a)))
          b (with-out-str (println (puget-str b)
                                   "is an instance of"
                                   (class b)))]
      (println (clojure.string/trim (indent a 10)))
      (print "     was: ")
      (println (indent b 10)))))

(defmethod prn-diffs ::diff-vecs
  [a b actual expected]
  (print-expected actual expected)
  (print "\n    diff:")
  (when a
    (print (ansi/sgr " - " :red))
    (let [a (with-out-str (cprint a))]
      (print (indent a 12))))
  (when b
    (print (ansi/sgr " + " :green))
    (let [b (with-out-str (cprint b))]
      (print (indent b 12))))
  (print "\n    hint: ")
  (println (list-diff actual expected)))

(defmethod prn-diffs :default
  [a b actual expected]
  (print-expected actual expected)
  (print "\n    diff:")
  (when a
    (print (ansi/sgr " - " :red))
    (let [a (with-out-str (cprint a))]
      (print (indent a 12))))
  (when b
    (print (ansi/sgr " + " :green))
    (let [b (with-out-str (cprint b))]
      (print (indent b 12))))
  (println))
