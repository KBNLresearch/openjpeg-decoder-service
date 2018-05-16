#include <jni.h>
#include <stdio.h>
#include <openjpeg.h>
#include "opj_res.h"
#include "nl_kb_jp2_Jp2Header.h"

static void setField
  (JNIEnv *env, jclass jcls, jobject obj, const char *methodName, int value) {
    jmethodID mid = (*env)->GetMethodID(env, jcls, methodName, "(I)V");
    (*env)->CallVoidMethod(env, obj, mid, value);
}

/*
 * Class:     jp2_Jp2Header
 * Method:    fromFile
 * Signature: (Ljava/lang/String;)Ljp2/Jp2Header;
 */
JNIEXPORT jobject JNICALL Java_nl_kb_jp2_Jp2Header_fromFile
  (JNIEnv *env, jclass jKlz, jstring jFilename) {

    const char *filename = (*env)->GetStringUTFChars(env, jFilename, NULL);
    if (NULL == filename) {
      return NULL;
    }

    struct opj_res resources;

    jclass jcls = (*env)->FindClass(env, "nl/kb/jp2/Jp2Header");
    jmethodID initMethod = (*env)->GetMethodID(env, jcls, "<init>", "()V");
    jobject obj = (*env)->NewObject(env, jcls, initMethod);

    opj_dparameters_t parameters;
    opj_set_default_decoder_parameters(&parameters);
    resources = opj_init(filename, &parameters);

    if (resources.status == 0) {
        opj_codestream_info_v2_t* info = opj_get_cstr_info(resources.l_codec);


        setField(env, jcls, obj, "setX1", resources.image->x1);
        setField(env, jcls, obj, "setY1", resources.image->y1);
        setField(env, jcls, obj, "setTw", info->tw);
        setField(env, jcls, obj, "setTh", info->th);
        setField(env, jcls, obj, "setTdx", info->tdx);
        setField(env, jcls, obj, "setTdy", info->tdy);
        setField(env, jcls, obj, "setNumRes", info->m_default_tile_info.tccp_info[0].numresolutions);
        setField(env, jcls, obj, "setNumComps", resources.image->numcomps);

        opj_destroy_cstr_info(&info);
        opj_cleanup(&resources);
        return obj;
    } else {
        opj_cleanup(&resources);
        jclass IOException = (*env)->FindClass(env, "java/io/IOException");
        (*env)->ThrowNew(env, IOException, "Failed to read header from file");
    }
}
