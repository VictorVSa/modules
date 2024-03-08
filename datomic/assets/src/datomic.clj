(ns <<ns-name>>.datomic
  (:import [java.io File])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [datomic.api :as d]
            [integrant.core :as ig]))

(defmethod ig/init-key :db.datomic/transactor
  [_ {:keys [datomic-path command args delay]}]
  (try
    (log/info "Starting Datomic Transactor")
    (let [process (->
                   (ProcessBuilder. (concat [command] (str/split args #",")))
                   (.directory (File. datomic-path))
                   (.start))]
      (Thread/sleep delay)
      (if (.isAlive process)
        (do
          (log/info "Datomic Transactor started")
          process)
        (log/error "Failed to start Datomic Transactor: process exited")))
    (catch Exception e (log/error e "Error starting Datomic Transactor"))))

(defmethod ig/halt-key! :db.datomic/transactor
  [_ transactor]
  (.destroyForcibly transactor))

(defmethod ig/init-key :db.datomic/conn
  [_ {:keys [db-uri]}]
  (when (d/create-database db-uri)
    (log/info (str "Database " db-uri " created (didn't exist)")))
  (d/connect db-uri))

(defmethod ig/halt-key! :db.datomic/conn
  [_ conn]
  (d/release conn))
