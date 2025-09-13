function bili_get(url,referer){
    // 定义 JS 对象
    let user = {
        url :url,
        referer:referer
    };

    // 转换为 JSON 字符串
    let jsonStr = JSON.stringify(user);
    var data=$.parseJSON(synchronousPostRequest(`../../../../data/ohupd/bili/get_proxy.json`,jsonStr));
    data.body=$.parseJSON(data.body);
    return data;
}
function check_error(data){
    data = data.body;
    if (data.code!=0){
        Swal.fire({
            title: 'error:'+data.code,
            text: data.message,
            icon: "error"
        });
        throw new DOMException("bili error"+data.code);
    }
}
function getCookieValue(head1, name) {
    const cookies = head1['set-cookie'];
    if (!cookies) return null;

    for (const cookie of cookies) {
        // 每个 cookie 字符串格式: "key=value; attr1; attr2; ..."
        const [keyValue] = cookie.split(';'); // 取第一个部分
        const [key, value] = keyValue.split('=');
        if (key === name) return decodeURIComponent(value); // 解码 URL 编码
    }
    return null;
}
