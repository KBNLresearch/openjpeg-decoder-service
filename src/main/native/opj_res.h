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

#define READ_FAILURE 0
#define READ_SUCCESS 1

typedef struct opj_res {
	int status;
	opj_stream_t *l_stream;
	opj_codec_t *l_codec;
	opj_image_t *image;
} opj_res_t;

void opj_cleanup(struct opj_res *resources);
struct opj_res opj_init_res(void);
struct opj_res opj_init(const char *fname, opj_dparameters_t *parameters);

int opj_init_from_stream(opj_dparameters_t *parameters, struct opj_res *resources);
