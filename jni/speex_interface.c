#include <jni.h>

#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "speex/speex.h"
#include "speex/speex_preprocess.h"

// Speex bit-packing struct
SpeexBits enc_bits;
// Speex encoder/decoder states
void *enc_state;
SpeexPreprocessState *preprocess_state;
int preprocess = 0;

struct Slot {
  SpeexBits	bits;
  void *state;
};

struct SlotVector
{
  struct Slot **slots;
  int nslots;
};

static struct SlotVector slots = {
	0,0
};

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_setPreprocessorEnable(JNIEnv *env, jclass cls, jboolean paramBoolean)
{
	if (paramBoolean) {
		jint frame_size;
		jint sampling_rate;

		speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &frame_size);
		speex_encoder_ctl(enc_state, SPEEX_GET_SAMPLING_RATE, &sampling_rate);

		preprocess = 1;
		preprocess_state = speex_preprocess_state_init(frame_size, sampling_rate);
	} else {
		preprocess = 0;
		speex_preprocess_state_destroy(preprocess_state);
	}
}

JNIEXPORT jboolean JNICALL Java_com_motolky_sound_SoundEncoder_createEncoder(JNIEnv *env, jobject obj)
{
	speex_bits_init(&enc_bits);
	enc_state = speex_encoder_init(&speex_nb_mode);
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_destroyEncoder(JNIEnv *env, jobject obj)
{
	speex_bits_destroy(&enc_bits);
	speex_encoder_destroy(enc_state);
}

JNIEXPORT jint JNICALL Java_com_motolky_sound_SoundEncoder_encode(JNIEnv *env, jobject obj, jshortArray paramArrayOfShort, jbyteArray paramArrayOfByte, jint paramInt)
{
	jint nbBytes;

	short* input_frame = (*env)->GetShortArrayElements(env, paramArrayOfShort, 0);
	speex_bits_reset(&enc_bits);
	if (preprocess)
		speex_preprocess_run(preprocess_state, input_frame);
	speex_encode_int(enc_state, input_frame, &enc_bits);
	(*env)->ReleaseShortArrayElements(env, paramArrayOfShort, input_frame, 0);

	char* output_frame = (*env)->GetByteArrayElements(env, paramArrayOfByte, 0);
	nbBytes = speex_bits_write(&enc_bits, output_frame, paramInt);
	(*env)->ReleaseByteArrayElements(env, paramArrayOfByte, output_frame, 0);
	return nbBytes;
}

JNIEXPORT jint JNICALL Java_com_motolky_sound_SoundEncoder_getEncoderComplexity(JNIEnv *env, jclass cls)
{
	jint complexity;
	speex_encoder_ctl(enc_state, SPEEX_GET_COMPLEXITY, &complexity);
	return complexity;
}

JNIEXPORT jint JNICALL Java_com_motolky_sound_SoundEncoder_getEncoderFrameSize(JNIEnv *env, jobject obj)
{
	jint frame_size;
	speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &frame_size);
	return frame_size;
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_setEncoderComplexity(JNIEnv *env, jclass cls, jint paramInt)
{
	speex_encoder_ctl(enc_state, SPEEX_SET_COMPLEXITY, &paramInt);
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_setEncoderDenoise(JNIEnv *env, jclass cls, jboolean paramBoolean)
{
	if (preprocess) {
		int state;
		if (paramBoolean) {
			state = 1;
		} else {
			state = 2;
		}
		speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_DENOISE, &state);
	}
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_setEncoderQuality(JNIEnv *env, jclass cls, jint paramInt)
{
	speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &paramInt);
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundEncoder_setEncoderVAD(JNIEnv *env, jclass cls, jboolean paramBoolean)
{
	speex_encoder_ctl(enc_state, SPEEX_SET_VAD, &paramBoolean);
}



JNIEXPORT jint JNICALL Java_com_motolky_sound_SoundDecoder_createDecoder(JNIEnv *env, jobject obj)
{
	int slot = allocate_slot(&slots);

	slots.slots[slot] = malloc(sizeof(struct Slot));

	struct Slot* gob = slots.slots[slot];

	speex_bits_init(&gob->bits);
	gob->state = speex_decoder_init(&speex_nb_mode);

	return slot;
}

JNIEXPORT jboolean JNICALL Java_com_motolky_sound_SoundDecoder_decode(JNIEnv *env, jobject obj, jint paramInt1, jbyteArray paramArrayOfByte, jint paramInt2, jshortArray paramArrayOfShort)
{
	char* input_frame = (*env)->GetByteArrayElements(env, paramArrayOfByte, 0);
	speex_bits_read_from(&slots.slots[paramInt1]->bits, input_frame, paramInt2);
	(*env)->ReleaseByteArrayElements(env, paramArrayOfByte, input_frame, 0);

	short* output_frame = (*env)->GetShortArrayElements(env, paramArrayOfShort, 0);
	speex_decode_int(slots.slots[paramInt1]->state, &slots.slots[paramInt1]->bits, output_frame);
	(*env)->ReleaseShortArrayElements(env, paramArrayOfShort, output_frame, 0);

	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_motolky_sound_SoundDecoder_destroyDecoder(JNIEnv *env, jobject obj, jint paramInt)
{
	speex_bits_destroy(&slots.slots[paramInt]->bits);
	speex_decoder_destroy(slots.slots[paramInt]->state);
	free(slots.slots[paramInt]);
	slots.slots[paramInt] = (void*)0;
}

JNIEXPORT jint JNICALL Java_com_motolky_sound_SoundDecoder_getDecoderFrameSize(JNIEnv *env, jobject obj, jint paramInt)
{
	jint frame_size;
	speex_decoder_ctl(slots.slots[paramInt]->state, SPEEX_GET_FRAME_SIZE, &frame_size);
	return frame_size;
}

int allocate_slot(struct SlotVector * sv)
{

    if (sv->slots==0) {
		sv->nslots = 1;
		sv->slots = malloc(sizeof(struct Slot*));
		sv->slots[0] = (void*)0;
    }

    int slot;

    for (slot=0; slot<sv->nslots; slot++) {
		if ((void*)0 == sv->slots[slot])
			break;
	}

	if (slot >= sv->nslots) {
		struct Slot** newArray = malloc( (1+sv->nslots)*sizeof(struct Slot*) );
		memcpy(newArray, sv->slots, sv->nslots * sizeof(struct Slot*));
		newArray[sv->nslots]=(void*)0;
		free(sv->slots);
		sv->slots = newArray;
		sv->nslots++;
	}

    return slot;
}
