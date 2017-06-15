(ns ultra.test
  (:use clojure.test)
  (:require [clojure.data :as data]
            [clojure.pprint :as pp]
            [io.aviso.repl :as pretty-repl]
            [puget.color.ansi :as ansi]
            [ultra.test.diff :as diff]
            [ultra.test.logic :as logic]))

(defn generate-diffs
  [a more]
  (map vector
       more
       (map #(take 2 (data/diff a %))
            more)))

(defn activate!
  {:added "0.3.3"}
  []
  (defmethod assert-expr '= [msg [_ a & more]]
  `(let [a# ~a]
     (if-let [more# (seq (list ~@more))]
       (let [result# (apply = a# more#)]
         (if result#
           (do-report {:type :pass, :message ~msg,
                       :expected a#, :actual more#})
           (do-report {:type :fail, :message ~msg,
                       :expected a#, :actual more#,
                       :diffs (generate-diffs a# more#)}))
         result#)
       (throw (Exception. "= expects more than one argument")))))

  (defmethod report :fail
    [{:keys [type expected actual diffs message] :as event}]
    (with-test-out
      (inc-report-counter :fail)
      (println (str (ansi/sgr "\nFAIL" :red) " in " (testing-vars-str event)))
      (when (seq *testing-contexts*) (println (testing-contexts-str)))
      (when message (println message))
      (if (seq diffs)
        (doseq [[actual [a b]] diffs
                :when (or a b)]
          (diff/prn-diffs a b actual expected))
        (diff/print-expected actual expected))
      (logic/maybe-print-values event)))

  (defmethod report :error
    [{:keys [message expected actual] :as event}]
    (with-test-out
      (inc-report-counter :error)
      (println (str (ansi/sgr "\nERROR" :red) " in " (testing-vars-str event)))
      (when (seq *testing-contexts*) (println (testing-contexts-str)))
      (when message (println message))
      (println "expected:" (pr-str expected))
      (print "  actual: ")
      (if (instance? Throwable actual)
        (pretty-repl/pretty-print-stack-trace actual)
        (prn actual))))

  (defmethod assert-expr :default [msg form]
    (cond
      (and (sequential? form) (logic/logic-ops (first form)))
      (logic/assert-logic msg form)
      (and (sequential? form) (function? (first form)))
      (logic/assert-predicate msg form)
      :else
      (assert-any msg form))))
