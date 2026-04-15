(ns clj-asu
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [babashka.json :as json]
   [clojure.string :as str]
   [clojure.edn :as edn]))

(defn login-request!
  [url body]
  (http/post url {:body (json/write-str body)}))

(defn login-token
  [user-input]
  (let [login-url (str/replace (:url user-input) "[ENV]" (:env user-input))]
    (-> (login-request! login-url {:username (:username user-input)
                                   :password (:password user-input)})
        :body
        json/read-str
        :ltpaToken)))

(defn backup-file! [path]
  (let [file    (fs/file path)
        backup  (fs/file (str path ".bak"))]
    (when (fs/exists? file)
      (fs/copy file backup {:replace-existing true})
      backup)))

;; Writes an exported variable in to rc-file
(defn set-exported-var!
  [rc-path var-name value]
  (let [file        (fs/file rc-path)
        content     (if (fs/exists? file) (slurp file) "")
        quoted-name (java.util.regex.Pattern/quote var-name)
        ;; Matches only:
        ;; export MY_VAR=...
        ;; with optional leading/trailing whitespace
        line-re     (re-pattern (str "(?m)^\\s*export\\s+" quoted-name "\\s*=.*$"))
        new-line    (str "export " var-name "=" (pr-str value))]
    ;; backup file in case something goes terribly wrong
    (backup-file! rc-path)
    (if (re-find line-re content)
      (spit file (str/replace content line-re new-line))
      (spit file
            (str content
                 (when (and (not (str/blank? content))
                            (not (str/ends-with? content "\n")))
                   "\n")
                 new-line
                 "\n")))))

(defn home-path
  [& parts]
  (apply fs/file (System/getProperty "user.home") parts))

(def asu-creds-edn ".asu-creds.edn")
(defn cli-options
  []
  (let [creds-edn (if (fs/exists? (home-path asu-creds-edn))
                    (-> asu-creds-edn
                        home-path
                        slurp
                        edn/read-string)
                    (throw (ex-info "Create a ~/.asu-creds.edn file with username and password for defaults." {:babashka/exit 1})))]
    {:username {:default (:username creds-edn)}
     :password {:default (:password creds-edn)}
     :url      {:default (:url creds-edn)}
     :env {:default "cloud"}}))

(def rc-file-name ".zshrc")

(defn run
  []
  (let [user-input  (cli/parse-opts *command-line-args* {:spec (cli-options)})
        token       (login-token user-input)]
    (-> rc-file-name
        home-path
        (set-exported-var! "ltpa_token" token))
    (println "Successfully wrote token into rc file. Source your shell env!")
    (println token)))

(run)
