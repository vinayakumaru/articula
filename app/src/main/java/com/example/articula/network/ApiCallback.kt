package com.example.articula.network

interface ApiCallback<T> {
    fun onResult(t : T)
}