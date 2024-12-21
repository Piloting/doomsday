package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.sql.EnumType;

/**
 * Состояния загрузки строки в задании
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum LoadState {
    NEW,
    SUCCESS,
    ERROR,
    SKIP
}
