package com.example.contactapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper : SQLiteOpenHelper {
    constructor(context: Context) : super(context, DATABASE_NAME, null, DATABASE_VERSION)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_TABLE)
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "contact.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "contact"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_PHONE = "phone"
        private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NAME TEXT, $COLUMN_PHONE TEXT)"
        private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

}