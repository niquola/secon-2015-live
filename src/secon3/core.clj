(ns secon3.core
  (:require
    [hiccup.core :as hc]
    [hiccup.page :as hp]
    [ring.middleware.defaults :as rmd]
    [org.httpkit.server :as ohs]))

;; start at 18:25
;; start at 18:57

(defn layout [& cnt]
  (hp/html5
    [:head
     (hp/include-css "/app.css")]
    [:body
     cnt
     (hp/include-js "/app.js")]))

(defn ok [body]
  {:status 200
   :body body
   :headers {"Content-Type" "text/html"}})

(defn index [req]
  (ok (layout [:textarea#inp] [:div#out])))

#_(def clients (atom #{}))

(defn broad-cast [msg]
  (doseq [ch @clients]
    (ohs/send! ch msg)))

(defn c [clr cnt]
  [:span {:style (str "color:" clr)} cnt])

(defn gray [x]
  (c "gray" x))

(defn hl [x]
  (cond
    (keyword? x) (c "cyan" (pr-str x))
    (string? x) (c "green" (pr-str x))
    (number? x) (c "red" (pr-str x))
    (vector? x) [:span (gray "[") (interpose " " (map hl x)) (gray "]")]
    (list? x) [:span (gray "(") (c "orange" (first x)) " " (interpose " " (map hl (rest x))) (gray ")")]
    (map? x) [:span (gray "{")
              (interpose " " (map (fn [[k v]]
                                    [:span (hl k) " " (hl v)]
                                    ) x))
              (gray "}")]
    :else (pr-str x)))

(defn myeval [s]
  (try
    (let [res (with-out-str
                (-> (read-string s)
                    (eval)
                    (pr-str)
                    (println)))]
      [:div.message [:pre (hl (read-string s))] [:pre res]])
    (catch Exception e
      [:div.message.error
       [:pre (with-out-str (clojure.stacktrace/print-stack-trace e))]])))

(defn repl [req]
  (ohs/with-channel req ch
    (println "New ch" ch)
    (swap! clients conj ch)
    (ohs/on-receive ch
                    (fn [msg]
                      (broad-cast (hc/html (myeval msg)))))
    (ohs/on-close ch (fn [_]
                       (println "Close ch" ch)
                       (swap! clients disj ch)))))

(defn not-found [req]
  (ok (layout [:h1 "Handler for " (:uri req) " not found"])))

(def routes
  {"/" index
   "/repl" repl})

(defn dispatch [{uri :uri :as req}]
  (let [h (get routes uri not-found)]
    (h req)))

(def app
  (-> dispatch
      (rmd/wrap-defaults rmd/site-defaults)))

(defn start [port]
  (def stop
    (ohs/run-server #'app {:port port})))


(comment

  (start 8080)
  (require '[vinyasa.pull :as vp])
  (vp/pull 'http-kit)
  (vp/pull 'hiccup)
  (vp/pull 'ring/ring-defaults)
  )
