(ns repl.clj.main
  (:require [pyro.printer :as pyro]
            [rebel-readline.clojure.main]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [clojure.stacktrace]
            [clojure.main]))

(def printer (expound/custom-printer {:print-specs? false
                                      :show-valid-values? true
                                      :theme :figwheel-theme}))

(defn repl-caught [e]
  (let [ex (clojure.main/repl-exception e)
        tr (.getStackTrace ex)
        el (when-not (zero? (count tr)) (aget tr 0))
        ex-m (Throwable->map ex)]
    (binding [*out* *err*]
      (cond
        ;; If the output is a clojure spec issue...
        (::s/problems (:data ex-m))
        ;; print expound output
        (do
          (println (str (re-find  #"Call to .* did not conform to spec\:" (.getMessage ex))
                        "\n"
                        (with-out-str (printer (:data ex-m))))))

        (instance? clojure.lang.LispReader$ReaderException e)
        (println (:cause (Throwable->map e)))

        :else
        ;; otherwise print exception
        (println (str (if (instance? clojure.lang.Compiler$CompilerException ex)
                        (str
                         (-> ex class .getSimpleName)
                         " " (.getMessage ex) " ")
                        (str " " (if el
                                   (clojure.stacktrace/print-stack-trace ex)
                                   "[trace missing]")))))))))


(defn -main []
  (set! s/*explain-out* printer)
  (st/instrument)
  (pyro/swap-stacktrace-engine!)
  (rebel-readline.clojure.main/repl :caught repl-caught))
