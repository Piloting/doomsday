// Use DBML to define your database structure
// Docs: https://dbml.dbdiagram.io/docs


Table "ddn_metric_value" {
  "metric_value_id" bigint [pk, not null]
  "metric" integer [not null, note: 'id метрики (enum Metric)']
  "period_type" integer [not null, note: 'тип периода. 0 - день, 1 - месяц, 2 - год (enum PeriodType)']
  "rss_date" date [not null, note: 'дата или первый день периода']
  "value_str" "character varying(1000)" [note: 'текстовое значение метрики']
  "value_num" numeric [note: 'числовое значение метрики']

  Indexes {
    (metric, period_type, rss_date) [type: btree, unique, name: "ddn_metric_value_idx_unique"]
  }
  Note: 'рассчитанные значения метрик'
}

Table "ddn_new" {
  "new_id" bigint [pk, not null]
  "new_date" timestamp [not null, note: 'дата со временем новости']
  "title" "character varying(1000)" [not null, note: 'заголовок новости']
  "link" "character varying(1000)" [not null, note: 'перманентная ссылка на новость']
  "guid" "character varying(1000)" [unique, not null, note: 'уникальный id новости']
  "rss_source_id" bigint [not null, note: 'ссылка на источник']

  Indexes {
    new_date [type: btree, name: "ddn_new_idx_date"]
  }
}

Table "ddn_new_word" {
  "new_id" bigint [not null, note: 'ссылка на новость']
  "word_id" bigint [not null, note: 'ссылка на слово из словаря']

  Indexes {
    (new_id, word_id) [pk, name: "ddn_new_word_pk"]
  }
  Note: 'привязка слов к новостям'
}

Table "ddn_rss_source" {
  "rss_source_id" bigint [pk, not null]
  "url" "character varying(500)" [unique, not null, note: 'ссылка на RSS']
  "source_name" "character varying(500)" [not null, note: 'название источника']
  "source_enabled" boolean [not null, note: 'вкл\\выкл источник']
  Note: 'источники новостей'
}

Table "ddn_task" {
  "task_id" bigint [pk, not null]
  "rss_date" date [not null, note: 'дата']
  "rss_source_id" bigint [not null, note: 'ID источника']
  "create_date" timestamp [not null, note: 'дата создания']
  "load_state" integer [not null, note: 'Статус загрузки 0 NEW, 1 SUCCESS, 2 ERROR']

  Indexes {
    load_state [type: btree, name: "ddn_task_is_load_state_idx"]
    (rss_source_id, rss_date) [type: btree, unique, name: "ddn_task_unique_idx"]
  }
  Note: 'Задание на загрузку RSS'
}

Table "ddn_word" {
  "word_id" bigint [pk, not null]
  "word" "character varying(100)" [unique, not null, note: 'слово в нормальной форме']
  Note: 'слова в нормальной форме'
}


Table "ddn_word_freq" {
  "word_freq_id" bigint [pk, not null]
  "word_id" bigint [not null, note: 'ссылка на слово из словаря']
  "rss_date" date [not null, note: 'дата или первый день периода']
  "period_type" integer [not null, note: 'тип периода. 0 - день, 1 - месяц, 2 - год']
  "word_count" integer [note: 'количество слов в этом периоде']

  Indexes {
    (rss_date, period_type, word_count) [type: btree, name: "ddn_word_freq_idx_date_count"]
    (word_id, rss_date, period_type) [type: btree, unique, name: "ddn_word_freq_idx_word_id"]
  }
  Note: 'количество слов в периоде - день\\месяц\\год'
}

Ref "fk_new_word_to_new":"ddn_new"."new_id" < "ddn_new_word"."new_id"

Ref "fk_new_word_to_word":"ddn_word"."word_id" < "ddn_new_word"."word_id"

Ref "fk_rss_source_id":"ddn_rss_source"."rss_source_id" < "ddn_task"."rss_source_id"

Ref "fk_word_freq_to_word":"ddn_word"."word_id" < "ddn_word_freq"."word_id"

Ref "new_to_source":"ddn_rss_source"."rss_source_id" < "ddn_new"."rss_source_id"
