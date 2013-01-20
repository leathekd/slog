(ns slog.utils
  (:import (java.net InetAddress Inet4Address NetworkInterface
                     UnknownHostException)))

(defn ip-address
  "Get the IP address of the local machine"
  []
  ;; http://stackoverflow.com/questions/8765578/
  ;;   get-local-ip-address-without-connecting-to-the-internet
  (->> (enumeration-seq (NetworkInterface/getNetworkInterfaces))
       (filter #(and (.isUp %) (not (.isLoopback %)) (not (.isVirtual %))))
       (first)
       (.getInterfaceAddresses)
       (map #(.getAddress %))
       (filter #(instance? Inet4Address %))
       (first)
       (.getHostAddress)))

(defn hostname
  "Get the host name of the local machine"
  []
  (try
    (.getHostName (InetAddress/getLocalHost))
    (catch UnknownHostException e
      (ip-address))))