#include <jni.h>
#include <stdio.h>
#include <openjpeg.h>
#include "opj_res.h"
#include "nl_kb_jp2_Jp2Decode.h"

static void setField
  (JNIEnv *env, jclass jcls, jobject obj, const char *methodName, int value) {
    jmethodID mid = (*env)->GetMethodID(env, jcls, methodName, "(I)V");
    (*env)->CallVoidMethod(env, obj, mid, value);
}

JNIEXPORT jobject JNICALL Java_nl_kb_jp2_Jp2Decode_decodeJp2Area
  (JNIEnv *env, jclass jKlz, jstring jFilename,
    jint x, jint y, jint w, jint h, jint cp_reduce,
    jobjectArray colorBands) {

    jclass jcls = (*env)->FindClass(env, "nl/kb/jp2/DecodedImageDims");
    jmethodID initMethod = (*env)->GetMethodID(env, jcls, "<init>", "()V");
    jobject obj = (*env)->NewObject(env, jcls, initMethod);

    const char *filename = (*env)->GetStringUTFChars(env, jFilename, NULL);
    if (NULL == filename) {
        jclass IOException = (*env)->FindClass(env, "java/io/IOException");
        (*env)->ThrowNew(env, IOException, "Failed to decode file");
        return obj;
    }

    struct opj_res resources;
    opj_dparameters_t parameters;
    opj_set_default_decoder_parameters(&parameters);
    parameters.cp_reduce = cp_reduce;

    resources = opj_init(filename, &parameters);
    int shouldThrow = 0;
    int numcomps = resources.image->numcomps;

    if (resources.status == 0) {
        opj_set_decode_area(resources.l_codec,
            resources.image,
            x, y,
            x + w, y + h);
        if (opj_decode(resources.l_codec, resources.l_stream, resources.image) &&
            opj_end_decompress(resources.l_codec, resources.l_stream)) {

            setField(env, jcls, obj, "setWidth", resources.image->comps[0].w);
            setField(env, jcls, obj, "setHeight", resources.image->comps[0].h);

            for (int i = 0; i < numcomps; i++) {
                jintArray band = (*env)->GetObjectArrayElement(env, colorBands, i);

                (*env)->SetIntArrayRegion(env, band, 0, resources.image->comps[i].w * resources.image->comps[i].h,
                    resources.image->comps[i].data);
            }
        } else {
            shouldThrow = 1;
        }
    } else {
        shouldThrow = 1;
    }
    opj_cleanup(&resources);

    if (shouldThrow) {
        jclass IOException = (*env)->FindClass(env, "java/io/IOException");
        (*env)->ThrowNew(env, IOException, "Failed to decode file");
    }
    return obj;
}
