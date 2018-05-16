/**
    jp2png-cgi
    Copyright (C) 2013  René van der Ark

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <openjpeg.h>
#include "log.h"
#include "opj_res.h"


static void error_callback(const char *msg, void *client_data) {(void)client_data; log_error(msg);}
static void warning_callback(const char *msg, void *client_data) { (void)client_data; log_warning(msg);}
static void info_callback(const char *msg, void *client_data) {(void)client_data; log_info(msg);}



struct opj_res opj_init_res(void) {
	struct opj_res resources;

	resources.status = -1;
	resources.l_stream = NULL;
	resources.l_codec = NULL;
	resources.image = NULL;

	return resources;
}

int opj_init_from_stream(opj_dparameters_t *parameters, struct opj_res *resources) {

	resources->image = NULL;
	resources->l_codec = opj_create_decompress(OPJ_CODEC_JP2);

	if(!opj_setup_decoder(resources->l_codec, parameters)) {
		opj_stream_destroy(resources->l_stream);
		opj_destroy_codec(resources->l_codec);
		resources->l_stream = NULL;
		resources->l_codec = NULL;
		return 2;
	}

	if(!opj_read_header(resources->l_stream, resources->l_codec, &(resources->image))) {
		opj_stream_destroy(resources->l_stream);
		opj_destroy_codec(resources->l_codec);
		opj_image_destroy(resources->image);
		resources->l_stream = NULL;
		resources->l_codec = NULL;
		resources->image = NULL;
		return 3;
	}

	if(getenv("JP2_VERBOSE") != NULL) {
		opj_set_info_handler(resources->l_codec, info_callback,00);
		opj_set_warning_handler(resources->l_codec, warning_callback,00);
	}

	if(getenv("JP2_SUPPRESS_ERRORS") == NULL) {
		opj_set_error_handler(resources->l_codec, error_callback,00);
	}

	return 0;
}

struct opj_res opj_init(const char *fname, opj_dparameters_t *parameters) {
	struct opj_res resources = opj_init_res();

	resources.l_stream = opj_stream_create_default_file_stream(fname,1);
	if(!resources.l_stream) {
		resources.status = 1;
		return resources;
	}
	resources.status = opj_init_from_stream(parameters, &resources);
	return resources;
}

void opj_cleanup(struct opj_res *resources) {
	if(resources->l_stream) { opj_stream_destroy(resources->l_stream); resources->l_stream = NULL; }
	if(resources->l_codec) { opj_destroy_codec(resources->l_codec);resources->l_codec = NULL; }
	if(resources->image) { opj_image_destroy(resources->image); resources->image = NULL; }
}
