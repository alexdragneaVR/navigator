(ns vr.macros)

(defmacro safe [& body]
  (let [e (gensym "e-")]
    `(try ~@body
      (catch :default ~e
        (vr.pi/add-error ~e)))))


(defmacro gosafe [& body]
  (let [e (gensym "e-")]
    `(cljs.core.async.macros/go
      (try ~@body
        (catch :default ~e
          (vr.pi/error :500 (ex-data ~e) ~e)
          (vr.pi/add-errors (ex-data ~e)))))))



(defmacro <? [& body]
  `(vr.pi/throw-err
     (cljs.core.async/<! ~@body)))
