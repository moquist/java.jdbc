;;  Copyright (c) Sean Corfield. All rights reserved. The use and
;;  distribution terms for this software are covered by the Eclipse Public
;;  License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
;;  be found in the file epl-v10.html at the root of this distribution.  By
;;  using this software in any fashion, you are agreeing to be bound by the
;;  terms of this license.  You must not remove this notice, or any other,
;;  from this software.
;;
;;  ddl.clj
;;
;;  A basic DDL DSL for use with clojure.java.jdbc (or you can use any
;;  other DDL DSL you want to...)
;;
;;  seancorfield (gmail)
;;  December 2013

(ns
  ^{:author "Sean Corfield",
    :doc "An optional DSL for generating DDL.

Intended to be used with clojure.java.jdbc, this provides a simple DSL -
Domain Specific Language - that generates raw DDL strings. Any other DSL
can be used instead. This DSL is entirely optional and is deliberately
not very sophisticated." }
  clojure.java.jdbc.ddl
  (:require [clojure.java.jdbc.sql :as sql]))

(defn create-table
  "Given a table name and column specs with an optional table-spec
   return the DDL string for creating that table."
  [name & specs]
  (let [col-specs (take-while (fn [s]
                                (not (or (= :table-spec s)
                                         (= :entities s)))) specs)
        other-specs (drop (count col-specs) specs)
        {:keys [table-spec entities] :or {entities sql/as-is}} other-specs
        table-spec-str (or (and table-spec (str " " table-spec)) "")
        specs-to-string (fn [specs]
                          (apply str
                                 (map (sql/as-str entities)
                                      (apply concat
                                             (interpose [", "]
                                                        (map (partial interpose " ") specs))))))]
    (format "CREATE TABLE %s (%s)%s"
            (sql/as-str entities name)
            (specs-to-string col-specs)
            table-spec-str)))

(defn drop-table
  "Given a table name, return the DDL string for dropping that table."
  [name & {:keys [entities] :or {entities sql/as-is}}]
  (format "DROP TABLE %s" (sql/as-str entities name)))

(defn create-index
  "Given an index name, table name, vector of column names, and
  (optional) is-unique, return the DDL string for creating an index.

   Examples:
   (create-index :indexname :tablename [:field1 :field2] :unique)
   \"CREATE UNIQUE INDEX indexname ON tablename (field1, field2)\"

   (create-index :indexname :tablename [:field1 :field2])
   \"CREATE INDEX indexname ON tablename (field1, field2)\""
  [index-name table-name cols & specs]
  (let [is-unique (seq (filter #(= :unique %) specs))
        entities-spec (drop-while #(not= :entities %) specs)
        {:keys [entities] :or {entities sql/as-is}} (take 2 entities-spec)
        cols-string (apply str
                           (interpose ", "
                                      (map (sql/as-str entities)
                                           cols)))
        is-unique (if is-unique "UNIQUE " "")]
    (format "CREATE %sINDEX %s ON %s (%s)"
            is-unique
            (sql/as-str entities index-name)
            (sql/as-str entities table-name)
            cols-string)))

(defn drop-index
  "Given an index name, return the DDL string for dropping that index."
  [name & {:keys [entities] :or {entities sql/as-is}}]
  (format "DROP INDEX %s" (sql/as-str entities name)))

(defn create-primary-key
  "Given a table name and a vector of column names, return the DDL
   string for creating a primary key.

   Example:
   (create-primary-key :tablename [:field1 :field2])
   \"ALTER TABLE tablename ADD PRIMARY KEY (field1, field2)\""
  [table-name cols & specs]
  (let [entities-spec (drop-while #(not= :entities %) specs)
        {:keys [entities] :or {entities sql/as-is}} (take 2 entities-spec)
        cols-string (apply str
                           (interpose ", "
                                      (map (sql/as-str entities)
                                           cols)))]
    (format "ALTER TABLE %s ADD PRIMARY KEY (%s)"
            (sql/as-str entities table-name)
            cols-string)))

(defn create-foreign-key
  "Given a foreign key spec, return the DDL string for creating the
   foreign key.

   Examples:
   (create-foreign-key :constraint-name :tablename :field1 :reftablename :reffield1)
   \"ALTER TABLE tablename ADD CONSTRAINT constraint-name FOREIGN KEY (field1) REFERENCES reftablename (reffield1)\"

   (create-foreign-key :ConstraintName :TableName :field1 :RefTableName :reffield1 :entities sql/lower-case)
   \"ALTER TABLE tablename ADD CONSTRAINT constraintname FOREIGN KEY (field1) REFERENCES reftablename (reffield1)\""
  [constraint-name table-name col ref-table-name ref-col & specs]
  (let [entities-spec (drop-while #(not= :entities %) specs)
        {:keys [entities] :or {entities sql/as-is}} (take 2 entities-spec)]
    (format "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)"
            (sql/as-str entities table-name)
            (sql/as-str entities constraint-name)
            (sql/as-str entities col)
            (sql/as-str entities ref-table-name)
            (sql/as-str entities ref-col))))
