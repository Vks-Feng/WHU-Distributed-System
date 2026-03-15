const result = document.getElementById("result");
const productIdInput = document.getElementById("productId");
const queryBtn = document.getElementById("queryBtn");

async function queryProduct() {
  const productId = Number(productIdInput.value);
  if (!Number.isInteger(productId) || productId <= 0) {
    result.textContent = "请输入合法商品 ID（正整数）";
    return;
  }

  result.textContent = "请求中...";
  try {
    const response = await fetch(`/api/v1/products/${productId}`);
    const data = await response.json();
    result.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    result.textContent = `请求失败: ${error}`;
  }
}

queryBtn.addEventListener("click", queryProduct);
