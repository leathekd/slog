{:slog
 {:loggers [] #_"can be one of :es, :log, or a seq of both"
  :es {:connection-url "http://localhost:9200"
       :index "slog"}}}