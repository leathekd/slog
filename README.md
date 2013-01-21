# slog

A Clojure library designed to create log messages as Clojure maps and
log them to a file or ElasticSearch for later processing and
inspection. It adds the concept of a "context" to the log messages in
an attempt to group messages for the same logical unit of work.

It is currently incomplete and really just in the thinking-out-loud
stage.  More to come if it pans out.

## Usage

```clojure
(require '[slog.core :as slog])

;; call new-context when starting a new unit of work
;; all log messages will have a :context key with a UUID making it
;; easier to group log output for logical units of work
(slog/new-context)

(slog/debug "something")
```

Look at resources/config.clj for the current config and use Carica to
set your own config for Slog.

test/log4j.properties offers a test appender for the Slog output so
that the Slog logs go to slog.log rather than littering the normal
log.

## Future

- Better docs.
- Look into performance impact.  While I may never recommend this for
  use in a tight loop, it'd be nice if it wasn't horrifically expensive.
- Queuing up the log messages in memory and flushing them all when the
  current "context" is marked as done.  Perhaps making is such that
  successful completion won't Slog anything.

  [Related pragpub article.](http://pragprog.com/magazines/2011-12/justintime-logging)

  This makes a lot more sense for ES than for file based logging.

## License

Copyright © 2013 David Leatherman

Distributed under the Eclipse Public License, the same as Clojure.
