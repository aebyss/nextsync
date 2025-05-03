#ifndef SYNC_H
#define SYNC_H

#include <stddef.h>

int upload_file(const char* local_path, const char* nextcloud_url, const char* username, const char* password);
void download_file(const char* nextcloud_url, const char* local_path, const char* username, const char* password);
int list_files(const char* url, const char* user, const char* pass, char* outBuffer, size_t outSize);
int list_folders(const char* remote_url, const char* username, const char* password);
size_t write_callback(void *ptr, size_t size, size_t nmemb, void *userdata);
int list_folders_into_buffer(const char* remote_url, const char* username, const char* password, char* outBuffer, size_t outSize);


#endif