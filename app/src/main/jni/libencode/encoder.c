/********************************************************************
 *                                                                  *
 * THIS FILE IS PART OF THE OggVorbis SOFTWARE CODEC SOURCE CODE.   *
 * USE, DISTRIBUTION AND REPRODUCTION OF THIS LIBRARY SOURCE IS     *
 * GOVERNED BY A BSD-STYLE SOURCE LICENSE INCLUDED WITH THIS SOURCE *
 * IN 'COPYING'. PLEASE READ THESE TERMS BEFORE DISTRIBUTING.       *
 *                                                                  *
 * THE OggVorbis SOURCE CODE IS (C) COPYRIGHT 1994-2007             *
 * by the Xiph.Org Foundation http://www.xiph.org/                  *
 *                                                                  *
 ********************************************************************

 function: simple example encoder
 last mod: $Id: encoder_example.c 16946 2010-03-03 16:12:40Z xiphmont $

 ********************************************************************/

/* takes a stereo 16bit 44.1kHz WAV file from stdin and encodes it into
   a Vorbis bitstream */

/* Note that this is POSIX, not ANSI, code */

#include <jni.h>
#include <android/log.h> 

#define LOG_TAG "ENCODER"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##args);

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <vorbis/vorbisenc.h>
#include <lame.h>

#ifdef _WIN32 /* We need the following two to set stdin/stdout to binary */
#include <io.h>
#include <fcntl.h>
#endif

#if defined(__MACOS__) && defined(__MWERKS__)
#include <console.h>      /* CodeWarrior's Mac "command-line" support */
#endif

#define READ 1024

static JavaVM *gJavaVM;
static jobject myobj = NULL;
static unsigned long seed = 0;
int jbool;

int runOgg(const char *ifname, const char *ofname, int ch, int rate) {

    int status;
    JNIEnv *env;
    int isAttached = 0;

    if (!myobj) return 0;

    if ((status = (*gJavaVM)->GetEnv(gJavaVM, (void**)&env, JNI_VERSION_1_6)) < 0) {
        if ((status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL)) < 0) {
            return 0;
        }
        isAttached = 1;
    }

	jclass cls = (*env)->GetObjectClass(env, myobj);
    if (!cls) {
        if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
        return 0;
    }
	jmethodID mid = (*env)->GetMethodID(env, cls, "updatePBar", "(I)V");
    if (!mid) {
        if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
        return 0;
    }

	FILE * infile;
	FILE * outfile;
	int fileSize = 0;
	int partsCount = 0;
	signed char readbuffer[READ*4+44]; /* out of the data segment, not the stack */
  
	if ((infile = fopen (ifname, "rb")) == NULL) {
		LOGD("Error: fopen failed");
		return 0;
	}
	if ((outfile = fopen (ofname, "wb")) == NULL) {
		LOGD("Error: fopen failed");
		return 0;
	}
  
	ogg_stream_state os; /* take physical pages, weld into a logical
                          stream of packets */
	ogg_page         og; /* one Ogg bitstream page.  Vorbis packets are inside */
	ogg_packet       op; /* one raw packet of data for decode */

	vorbis_info      vi; /* struct that stores all the static vorbis bitstream
                          settings */
	vorbis_comment   vc; /* struct that stores all the user comments */

	vorbis_dsp_state vd; /* central working state for the packet->PCM decoder */
	vorbis_block     vb; /* local working space for packet->PCM decode */

	int eos=0,ret;
	int i, founddata;

#if defined(macintosh) && defined(__MWERKS__)
	int argc = 0;
	char **argv = NULL;
	argc = ccommand(&argv);	/* get a "command line" from the Mac user */
							/* this also lets the user set stdin and stdout */
#endif

  /* we cheat on the WAV header; we just bypass 44 bytes (simplest WAV
     header is 44 bytes) and assume that the data is 44.1khz, stereo, 16 bit
     little endian pcm samples. This is just an example, after all. */

#ifdef _WIN32 /* We need to set stdin/stdout to binary mode. Damn windows. */
  /* if we were reading/writing a file, it would also need to in
     binary mode, eg, fopen("file.wav","wb"); */
  /* Beware the evil ifdef. We avoid these where we can, but this one we
     cannot. Don't add any more, you'll probably go to hell if you do. */
	_setmode( _fileno( stdin ), _O_BINARY );
	_setmode( _fileno( stdout ), _O_BINARY );
#endif


  /* we cheat on the WAV header; we just bypass the header and never
     verify that it matches 16bit/stereo/44.1kHz.  This is just an
     example, after all. */

	readbuffer[0] = '\0';
	for (i=0, founddata=0; i<30 && ! feof(infile) && ! ferror(infile); i++)
	{
		fread(readbuffer,1,2,infile);
		if ( ! strncmp((char*)readbuffer, "da", 2) ){
			founddata = 1;
			fread(readbuffer,1,6,infile);
			break;
		}
	}


  /********** Encode setup ************/

	vorbis_info_init(&vi);

  /* choose an encoding mode.  A few possibilities commented out, one
     actually used: */

  /*********************************************************************
   Encoding using a VBR quality mode.  The usable range is -.1
   (lowest quality, smallest file) to 1. (highest quality, largest file).
   Example quality mode .4: 44kHz stereo coupled, roughly 128kbps VBR

   ret = vorbis_encode_init_vbr(&vi,2,44100,.4);

   ---------------------------------------------------------------------

   Encoding using an average bitrate mode (ABR).
   example: 44kHz stereo coupled, average 128kbps VBR

   ret = vorbis_encode_init(&vi,2,44100,-1,128000,-1);

   ---------------------------------------------------------------------

   Encode using a quality mode, but select that quality mode by asking for
   an approximate bitrate.  This is not ABR, it is true VBR, but selected
   using the bitrate interface, and then turning bitrate management off:

   ret = ( vorbis_encode_setup_managed(&vi,2,44100,-1,128000,-1) ||
           vorbis_encode_ctl(&vi,OV_ECTL_RATEMANAGE2_SET,NULL) ||
           vorbis_encode_setup_init(&vi));

   *********************************************************************/

   
	ret=vorbis_encode_init_vbr(&vi,ch,rate,0.2);
  //ret = vorbis_encode_init(&vi,1,22050,-1,96000,-1);

  /* do not continue if setup failed; this can happen if we ask for a
     mode that libVorbis does not support (eg, too low a bitrate, etc,
     will return 'OV_EIMPL') */

	if(ret < 0) return 0;

  /* add a comment */
	vorbis_comment_init(&vc);
	vorbis_comment_add_tag(&vc,"ENCODER","ogg vorbis encoder");

  /* set up the analysis state and auxiliary encoding storage */
	vorbis_analysis_init(&vd,&vi);
	vorbis_block_init(&vd,&vb);

  /* set up our packet->stream encoder */
  /* pick a random serial number; that way we can more likely build
     chained streams just by concatenation */

	seed = ((seed * 214013L + 2531011L) >> 16) & 0x7fff;
	int r = (int) seed;
	ogg_stream_init(&os, r);

  /* Vorbis streams begin with three headers; the initial header (with
     most of the codec setup parameters) which is mandated by the Ogg
     bitstream spec.  The second header holds any comment fields.  The
     third header holds the bitstream codebook.  We merely need to
     make the headers, then pass them to libvorbis one at a time;
     libvorbis handles the additional Ogg bitstream constraints */

	{
		ogg_packet header;
		ogg_packet header_comm;
		ogg_packet header_code;

		vorbis_analysis_headerout(&vd,&vc,&header,&header_comm,&header_code);
		ogg_stream_packetin(&os,&header); /* automatically placed in its own
                                         page */
		ogg_stream_packetin(&os,&header_comm);
		ogg_stream_packetin(&os,&header_code);

    /* This ensures the actual
     * audio data will start on a new page, as per spec
     */
		while(!eos){
			int result=ogg_stream_flush(&os,&og);
			if(result==0)break;
			fwrite(og.header,1,og.header_len,outfile);
			fwrite(og.body,1,og.body_len,outfile);
		}

	}

	fileSize = fsize(infile);
	partsCount = fileSize / (READ*4);

	int aaa = 0;
	int progress;

	while(!eos && !jbool){
		long i;
		long bytes=fread(readbuffer,1,READ*4,infile); /* stereo hardwired here */

		if(bytes==0){
      /* end of file.  this can be done implicitly in the mainline,
         but it's easier to see here in non-clever fashion.
         Tell the library we're at end of stream so that it can handle
         the last frame and mark end of stream in the output properly */
			vorbis_analysis_wrote(&vd,0);

		}else{
			aaa++;
			if (aaa % (partsCount < 10 ? 1 : partsCount / 10) == 0) {
				progress = (100 * aaa) / (partsCount < 1 ? 1 : partsCount);
				(*env)->CallVoidMethod(env, myobj, mid, progress);
			}
      /* data to encode */

      /* expose the buffer to submit data */
			float **buffer=vorbis_analysis_buffer(&vd,READ*2/ch);

      /* uninterleave samples */
			for(i=0;i<bytes/(2*ch);i++){
				buffer[0][i]=((readbuffer[i*2*ch+1]<<8)|(0x00ff&(int)readbuffer[i*2*ch]))/32768.f;
				if (ch == 2)
					buffer[1][i]=((readbuffer[i*2*ch+3]<<8)|(0x00ff&(int)readbuffer[i*2*ch+2]))/32768.f;
			}

      /* tell the library how much we actually submitted */
			vorbis_analysis_wrote(&vd,i);
		}

    /* vorbis does some data preanalysis, then divvies up blocks for
       more involved (potentially parallel) processing.  Get a single
       block for encoding now */
		while(vorbis_analysis_blockout(&vd,&vb)==1){

      /* analysis, assume we want to use bitrate management */
			vorbis_analysis(&vb,NULL);
			vorbis_bitrate_addblock(&vb);

			while(vorbis_bitrate_flushpacket(&vd,&op)){

        /* weld the packet into the bitstream */
				ogg_stream_packetin(&os,&op);

        /* write out pages (if any) */
				while(!eos){
					int result=ogg_stream_pageout(&os,&og);
					if(result==0)break;
					fwrite(og.header,1,og.header_len,outfile);
					fwrite(og.body,1,og.body_len,outfile);

          /* this could be set above, but for illustrative purposes, I do
             it here (to show that vorbis does know where the stream ends) */

					if(ogg_page_eos(&og))eos=1;
				}
			}
		}
	}

  /* clean up and exit.  vorbis_info_clear() must be called last */

	fclose (outfile) ;
	fclose (infile) ;

	ogg_stream_clear(&os);
	vorbis_block_clear(&vb);
	vorbis_dsp_clear(&vd);
	vorbis_comment_clear(&vc);
	vorbis_info_clear(&vi);

  /* ogg_page and ogg_packet structs always point to storage in
     libvorbis.  They're never freed or manipulated directly */

	if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
	return 1;
}

int runMp3(const char *ifname, const char *ofname, int ch, int rate) {

    int status;
    JNIEnv *env;
    int isAttached = 0;

    if (!myobj) return 0;

    if ((status = (*gJavaVM)->GetEnv(gJavaVM, (void**)&env, JNI_VERSION_1_6)) < 0) {
        if ((status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL)) < 0) {
            return 0;
        }
        isAttached = 1;
    }

	jclass cls = (*env)->GetObjectClass(env, myobj);
    if (!cls) {
        if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
        return 0;
    }
	jmethodID mid = (*env)->GetMethodID(env, cls, "updatePBar", "(I)V");
    if (!mid) {
        if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
        return 0;
    }

	FILE *pcm;
	if ((pcm = fopen(ifname, "rb")) == NULL) {
		LOGD("Error: fopen failed");
		return 0;
	}
	FILE *mp3;
	if ((mp3 = fopen(ofname, "wb")) == NULL) {
		LOGD("Error: fopen failed");
		return 0;
	}

	int read, write;
	int fileSize = 0;
	int partsCount = 0;
	const int PCM_SIZE = 8192;
	const int MP3_SIZE = 8192;
	short int pcm_buffer[PCM_SIZE * ch];
	unsigned char mp3_buffer[MP3_SIZE];

	lame_t lame = lame_init();
	lame_set_num_channels(lame, ch);
	lame_set_in_samplerate(lame, rate);
	lame_set_VBR(lame, vbr_off);
	lame_init_params(lame);

	signed char header[44];
	fread(header, 1, 44, pcm);

	fileSize = fsize(pcm);

	partsCount = fileSize / (sizeof(short int) * ch * PCM_SIZE);

	int aaa = 0;
	int progress;

	do {
		read = fread(pcm_buffer, sizeof(short int) * ch, PCM_SIZE, pcm);
		if (read == 0 || read < (PCM_SIZE/4)) {
			write = lame_encode_flush(lame, mp3_buffer, MP3_SIZE);
		} else {
			aaa++;
			if (aaa % (partsCount < 10 ? 1 : partsCount / 10) == 0) {
				progress = (100 * aaa) / (partsCount < 1 ? 1 : partsCount);
				(*env)->CallVoidMethod(env, myobj, mid, progress);
			}
			if (ch == 1) {
				write = lame_encode_buffer(lame, pcm_buffer, pcm_buffer, read, mp3_buffer, MP3_SIZE);
			} else {
				write = lame_encode_buffer_interleaved(lame, pcm_buffer, read, mp3_buffer, MP3_SIZE);
			}
		}
		fwrite(mp3_buffer, write, 1, mp3);
	} while (read != 0 && !jbool);

	lame_close(lame);
	fclose(mp3);
	fclose(pcm);

	if (isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
	return 1;
}

JNIEXPORT jint JNICALL Java_org_txt_to_audiofile_TTSaverService_encodeOgg(JNIEnv *env,
	jobject jobj, jstring input_path, jstring output_path,jint in_num_channels, jint in_samplerate) {
	const char *in = (*env)->GetStringUTFChars(env, input_path, NULL);
	const char *out = (*env)->GetStringUTFChars(env, output_path, NULL);
	myobj = (*env)->NewGlobalRef(env, jobj);
	jbool = 0;
	int x = runOgg(in, out, in_num_channels, in_samplerate);
	(*env)->ReleaseStringUTFChars(env, input_path, in);
	(*env)->ReleaseStringUTFChars(env, output_path, out);
	(*env)->DeleteGlobalRef(env, myobj);
	myobj = NULL;
	return x;
}

JNIEXPORT jint JNICALL Java_org_txt_to_audiofile_TTSaverService_encodeMP3(JNIEnv *env,
	jobject jobj, jstring input_path, jstring output_path,jint in_num_channels, jint in_samplerate) {
	const char *in = (*env)->GetStringUTFChars(env, input_path, NULL);
	const char *out = (*env)->GetStringUTFChars(env, output_path, NULL);
	myobj = (*env)->NewGlobalRef(env, jobj);
	jbool = 0;
	int x = runMp3(in, out, in_num_channels, in_samplerate);
	(*env)->ReleaseStringUTFChars(env, input_path, in);
	(*env)->ReleaseStringUTFChars(env, output_path, out);
	(*env)->DeleteGlobalRef(env, myobj);
	myobj = NULL;
	return x;
}

JNIEXPORT void JNICALL Java_org_txt_to_audiofile_TTSaverService_cancelEnc(JNIEnv *env,
		jobject jobj) {
	jbool = 1;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJavaVM = vm;
    seed = (unsigned long) time(NULL);
    return JNI_VERSION_1_6;
}

int fsize(FILE *fp){
    int prev=ftell(fp);
    fseek(fp, 0L, SEEK_END);
    int sz=ftell(fp);
    fseek(fp,prev,SEEK_SET); //go back to where we were
    return sz;
}
