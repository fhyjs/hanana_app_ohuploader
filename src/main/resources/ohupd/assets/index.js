$('#content-iframe').on('load', function() {
    var iframe = $('#content-iframe')[0];
    var ifdoc = $(iframe.contentWindow.document);
    if (iframe.contentWindow.location.href.includes("pages/app/pages.html")){
        ifdoc.find("#links").append(`<li><a href='../../../ohupd/pages/index.html'>ottoHub转载工具</a></li>`);
    }
});
function getQueryParam(key) {
    return new URLSearchParams(window.location.search).get(key);
}
if (getQueryParam("act")=="ohupd"){
    setPage("../ohupd/pages/index.html");
}
