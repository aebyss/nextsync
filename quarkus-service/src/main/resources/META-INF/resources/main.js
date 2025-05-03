document.getElementById("uploadForm").addEventListener("submit", async function (e) {
    e.preventDefault();

    const file = document.getElementById("file").files[0];
    const remoteFolder = document.getElementById("remoteFolder").value.trim();
    const url = document.getElementById("url").value.trim();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const cleanRemoteFolder = remoteFolder.replace(/^\/+|\/+$/g, "");
    const remotePath = cleanRemoteFolder ? `${cleanRemoteFolder}/${file.name}` : file.name;


    console.log("🌐 Remote path:", remotePath);
    console.log("🌐 Base URL:", url);

    const res = await fetch(`/api/upload/${encodeURIComponent(file.name)}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/octet-stream",
            "X-Remote-Path": remotePath,
            "X-URL": url,
            "X-User": username,
            "X-Pass": password
        },
        body: file
    });

    const text = await res.text();
    document.getElementById("result").textContent = text;
});

async function loadFolders() {
    const url = document.getElementById("url").value.trim();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const res = await fetch(`/api/folders?url=${encodeURIComponent(url)}`, {
        method: "GET",
        headers: {
            "X-User": username,
            "X-Pass": password
        }
    });

    const folders = await res.json();
    const select = document.getElementById("remoteFolder");
    select.innerHTML = "";
    folders.forEach(folder => {
        const opt = document.createElement("option");
        opt.value = folder;
        opt.textContent = folder;
        select.appendChild(opt);
    });
}
