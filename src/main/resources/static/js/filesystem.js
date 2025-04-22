document.addEventListener("DOMContentLoaded", function () {
    const filesystemElement = document.querySelector("#filesystem .tree");

    document.addEventListener("snapshot-update", function (event) {
        const snapshot = event.detail;
        if (snapshot.fileDirectory) {
            filesystemElement.innerText = snapshot.fileDirectory;
        }
    });
});
