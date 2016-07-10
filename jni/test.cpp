#include <string.h>
#include <jni.h>
#include <android/log.h>

 extern "C" {
     JNIEXPORT jstring JNICALL Java_com_example_test_MainActivity_stringFromJNICPP(JNIEnv * env, jobject obj);
 };

 JNIEXPORT jstring JNICALL Java_com_example_test_MainActivity_stringFromJNICPP(JNIEnv * env, jobject obj)
 {

     return env->NewStringUTF("Hello From CPP");

 }

