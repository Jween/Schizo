package com.meizu.flyme.schizo.sample.service.bean;

/**
 * Created by Jwn on 2018/1/18.
 */

public class Book {

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }

    String title;
    String author;
}
