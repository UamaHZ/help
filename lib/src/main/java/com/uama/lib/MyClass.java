package com.uama.lib;

public class MyClass {
    static int count = 0;
    public static void main(String args[]){

        for(int i=0;i<30;i++){

            new Thread(){
                @Override
                public void run() {
                    count++;
                    System.out.println(count);
                }
            }.start();

            new Thread(){
                @Override
                public void run() {
                    count++;
                    System.out.println(count);
                }
            }.start();

            new Thread(){
                @Override
                public void run() {
                    count++;
                    System.out.println(count);
                }
            }.start();
        }

    }

    static void count(){

    }
}
