(ns slog.utils
  (:import (java.net InetAddress Inet4Address NetworkInterface
                     UnknownHostException)
           (java.util Date UUID)))

(defn ip-address
  "Get the IP address of the local machine"
  []
  (.getHostAddress (InetAddress/getLocalHost)))

(defn hostname
  "Get the host name of the local machine"
  []
  (try
    (.getHostName (InetAddress/getLocalHost))
    (catch UnknownHostException e
      (ip-address))))

(defn uuid []
  (str (UUID/randomUUID)))

(defn now []
  (Date.))
