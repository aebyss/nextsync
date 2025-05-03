#include <curl/curl.h>
#include <stdio.h>
#include <stddef.h>
#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <libxml/parser.h>
#include <libxml/tree.h>

struct MemoryStruct {
    char *memory;
    size_t size;
};

// Optional: log each read
static size_t read_callback(void *ptr, size_t size, size_t nmemb, void *stream) {
    FILE *f = (FILE *)stream;
    size_t n = fread(ptr, size, nmemb, f);
    printf("📤 [C] Read %zu bytes from file\n", n);
    return n;
}

int upload_file(const char* local_path, const char* nextcloud_url, const char* username, const char* password) {
    if (!local_path || !nextcloud_url || !username || !password) {
        fprintf(stderr, "❌ Missing parameters in upload_file\n");
        return -3;
    }

    CURL *curl = curl_easy_init();
    if (!curl) return -1;

    FILE *file = fopen(local_path, "rb");
    if (!file) {
        fprintf(stderr, "❌ Failed to open file: %s\n", local_path);
        return -2;
    }

    // Determine file size
    fseek(file, 0, SEEK_END);
    curl_off_t filesize = ftell(file);
    fseek(file, 0, SEEK_SET);
    printf("📦 [C] File to upload: %s (%lld bytes)\n", local_path, (long long)filesize);

    // Prepare credentials
    char userpwd[512];
    snprintf(userpwd, sizeof(userpwd), "%s:%s", username, password);

    // Set libcurl options
    curl_easy_setopt(curl, CURLOPT_URL, nextcloud_url);
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt(curl, CURLOPT_USERPWD, userpwd);
    curl_easy_setopt(curl, CURLOPT_READDATA, file);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, read_callback);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, filesize);
    curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L); // Debug output

    CURLcode res = curl_easy_perform(curl);
    printf("📶 [C] curl_easy_perform result: %d\n", res);

    fclose(file);
    curl_easy_cleanup(curl);

    return (int)res;
}


void download_file(const char* nextcloud_url, const char* local_path, const char* username, const char* password) {
    CURL *curl = curl_easy_init();
    if (curl) {
        FILE *file = fopen(local_path, "wb");
        if (!file) {
            fprintf(stderr, "Failed to open file for download: %s\n", local_path);
            return;
        }

        curl_easy_setopt(curl, CURLOPT_URL, nextcloud_url);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, NULL); // default write
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, file);
        curl_easy_setopt(curl, CURLOPT_USERPWD, username);
        curl_easy_setopt(curl, CURLOPT_PASSWORD, password);

        curl_easy_perform(curl);

        fclose(file);
        curl_easy_cleanup(curl);
    }
    }

 static size_t write_callback(void *ptr, size_t size, size_t nmemb, void *userdata) {
     FILE *file = (FILE *)userdata;
     return fwrite(ptr, size, nmemb, file);
 }

 int list_folders(const char* remote_url, const char* username, const char* password) {
     CURL *curl = curl_easy_init();
     if (!curl) return -1;

     FILE *output = fopen("/tmp/folder_list.xml", "wb"); // temp file for XML response
     if (!output) return -2;

     char userpwd[512];
     snprintf(userpwd, sizeof(userpwd), "%s:%s", username, password);

     struct curl_slist *headers = NULL;
     headers = curl_slist_append(headers, "Depth: 1");

     curl_easy_setopt(curl, CURLOPT_URL, remote_url);
     curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "PROPFIND");
     curl_easy_setopt(curl, CURLOPT_USERPWD, userpwd);
     curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
     curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
     curl_easy_setopt(curl, CURLOPT_WRITEDATA, output);

     CURLcode res = curl_easy_perform(curl);
     fclose(output);
     curl_slist_free_all(headers);
     curl_easy_cleanup(curl);

     return (int)res;
 }

static size_t write_memory(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *)userp;

    char *ptr = realloc(mem->memory, mem->size + realsize + 1);
    if (!ptr) return 0;

    mem->memory = ptr;
    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

int list_folders_into_buffer(const char* remote_url, const char* username, const char* password, char* outBuffer, size_t outSize) {
    CURL *curl = curl_easy_init();
    if (!curl) return -1;

    struct MemoryStruct chunk = { .memory = malloc(1), .size = 0 };

    char userpwd[512];
    snprintf(userpwd, sizeof(userpwd), "%s:%s", username, password);

    struct curl_slist *headers = NULL;
    headers = curl_slist_append(headers, "Depth: 1");

    curl_easy_setopt(curl, CURLOPT_URL, remote_url);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "PROPFIND");
    curl_easy_setopt(curl, CURLOPT_USERPWD, userpwd);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_memory);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&chunk);

    CURLcode res = curl_easy_perform(curl);
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK || chunk.memory == NULL) {
        free(chunk.memory);
        return -2;
    }

    xmlDoc *doc = xmlReadMemory(chunk.memory, chunk.size, "noname.xml", NULL, 0);
    free(chunk.memory);
    if (!doc) return -3;

    xmlNode *root = xmlDocGetRootElement(doc);
    xmlNode *node = NULL;
    size_t total_written = 0;

    for (node = root->children; node; node = node->next) {
        if (node->type == XML_ELEMENT_NODE &&
            strcmp((const char*)node->name, "response") == 0) {

            xmlNode *hrefNode = NULL;
            xmlNode *resourcetypeNode = NULL;
            int isCollection = 0;
            char *folderPath = NULL;

            for (xmlNode *child = node->children; child; child = child->next) {
                if (child->type == XML_ELEMENT_NODE) {
                    if (strcmp((const char*)child->name, "href") == 0) {
                        hrefNode = child;
                    } else if (strcmp((const char*)child->name, "propstat") == 0) {
                        for (xmlNode *statChild = child->children; statChild; statChild = statChild->next) {
                            if (statChild->type == XML_ELEMENT_NODE &&
                                strcmp((const char*)statChild->name, "prop") == 0) {
                                for (xmlNode *propChild = statChild->children; propChild; propChild = propChild->next) {
                                    if (propChild->type == XML_ELEMENT_NODE &&
                                        strcmp((const char*)propChild->name, "resourcetype") == 0) {
                                        for (xmlNode *rscChild = propChild->children; rscChild; rscChild = rscChild->next) {
                                            if (rscChild->type == XML_ELEMENT_NODE &&
                                                strcmp((const char*)rscChild->name, "collection") == 0) {
                                                isCollection = 1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isCollection && hrefNode) {
                const char *href = (const char*)xmlNodeGetContent(hrefNode);
                const char *prefix = "/remote.php/dav/files/admin/";
                if (strstr(href, prefix)) {
                    const char *rel = href + strlen(prefix);
                    size_t len = strlen(rel);
                    if (len > 0 && rel[len - 1] == '/') {
                        len--; // remove trailing slash
                    }

                    if (len > 0 && total_written + len + 2 < outSize) {
                        strncpy(outBuffer + total_written, rel, len);
                        total_written += len;
                        outBuffer[total_written++] = '\n';
                        outBuffer[total_written] = '\0';
                    }
                }
            }
        }
    }

    xmlFreeDoc(doc);
    return 0;
}