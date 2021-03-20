(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'nvlasscom.core
   :output-to "out/nvlasscom.js"
   :output-dir "out"})
