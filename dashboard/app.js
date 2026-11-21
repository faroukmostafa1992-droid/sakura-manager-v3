// Preview login gate. Client-side only; not suitable for production security.
const PREVIEW_PASSWORD_HASH="78237b79b29e8c06d5505818e4080b8b9c63c3d13c1ffc33205b29c934e43c46";
const AUTH_KEY="sakura-manager-preview-auth";
const loginGate=document.getElementById("loginGate");const shell=document.querySelector(".shell");
function setPreviewAuth(on){localStorage.setItem(AUTH_KEY,on?"1":"0");loginGate.classList.toggle("hidden",on);shell.classList.toggle("hidden",!on)}
setPreviewAuth(localStorage.getItem(AUTH_KEY)==="1");
document.getElementById("loginSubmit").addEventListener("click",async()=>{const email=document.getElementById("loginEmail").value.trim(),password=document.getElementById("loginPassword").value,err=document.getElementById("loginError");if(!email||!email.includes("@")){err.textContent="أدخل بريدًا إلكترونيًا صحيحًا";return}const digest=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(password));const hash=[...new Uint8Array(digest)].map(x=>x.toString(16).padStart(2,"0")).join("");if(hash!==PREVIEW_PASSWORD_HASH){err.textContent="كلمة المرور غير صحيحة";return}err.textContent="";setPreviewAuth(true)});
document.getElementById("loginPassword").addEventListener("keydown",e=>{if(e.key==="Enter")document.getElementById("loginSubmit").click()});
// Sakura Manager — demo-only local data model. Not connected to production database.
const statusNames={new:"جديد",confirmed:"تم التأكيد",preparing:"قيد التجهيز",delivery:"خرج للتوصيل",delivered:"تم التسليم",cancelled:"ملغي"};
const statusClass={new:"s-new",confirmed:"s-confirmed",preparing:"s-preparing",delivery:"s-delivery",delivered:"s-delivered",cancelled:"s-cancelled"};
const seedProducts=[
{name:"Arencia Booster Shot",type:"Mini Size · 10ml",price:350,cost:170,stock:4.66,icon:"🧴"},
{name:"Dr. Althea Relief Cream",type:"Mini Size · 10ml",price:250,cost:140,stock:1.25,icon:"🫧"},
{name:"Seoul 1988",type:"Mini Size · 10ml",price:250,cost:125,stock:3,icon:"🌷"},
{name:"Tirtir Ceramic Milk",type:"Mini Size · 10ml",price:400,cost:200,stock:1,icon:"🥛"},
{name:"Anua",type:"Mini Size · 10ml",price:350,cost:175,stock:1,icon:"🌿"},
{name:"Arencia Booster Shot",type:"Full Size",price:750,cost:390,stock:2,icon:"✨"}];
const seedOrders=[
{id:"#SK-2048",name:"سارة أحمد",items:[{product:0,qty:1}],shipping:70,status:"new"},
{id:"#SK-2047",name:"نور محمد",items:[{product:1,qty:2}],shipping:70,status:"confirmed"},
{id:"#SK-2046",name:"مريم خالد",items:[{product:2,qty:1},{product:4,qty:1}],shipping:70,status:"preparing"},
{id:"#SK-2045",name:"ملك حسن",items:[{product:3,qty:1}],shipping:70,status:"delivery"},
{id:"#SK-2044",name:"ندى علي",items:[{product:0,qty:2}],shipping:70,status:"delivered",stockApplied:true},
{id:"#SK-2043",name:"ريم محمود",items:[{product:5,qty:1}],shipping:70,status:"cancelled"}];
let state=(()=>{try{return JSON.parse(localStorage.getItem("sakura-manager-demo-v2"))||{products:seedProducts,orders:seedOrders,expenses:[]}}catch{return {products:seedProducts,orders:seedOrders,expenses:[]}}})();
const products=state.products,orders=state.orders,expenses=state.expenses||[];
function save(){localStorage.setItem("sakura-manager-demo-v2",JSON.stringify({products,orders,expenses}))}
let snapshotsAdded=false;orders.forEach(o=>o.items.forEach(i=>{if(i.unitPrice===undefined){i.unitPrice=products[i.product]?.price||0;snapshotsAdded=true}if(i.unitCost===undefined){i.unitCost=products[i.product]?.cost||0;snapshotsAdded=true}}));if(snapshotsAdded)save();
function money(n){return Math.round(n||0).toLocaleString("en-US")+" ج.م"}
function orderRevenue(o){return o.items.reduce((s,i)=>s+(i.unitPrice??products[i.product]?.price??0)*i.qty,0)}
function orderCost(o){return o.items.reduce((s,i)=>s+(i.unitCost??products[i.product]?.cost??0)*i.qty,0)}
function badge(s){return '<span class="status-pill '+statusClass[s]+'">'+statusNames[s]+'</span>'}
function renderRecent(){document.querySelector("#recentOrders").innerHTML=orders.slice().reverse().slice(0,4).map(o=>'<tr><td class="order-id">'+o.id+'</td><td>'+o.name+'</td><td>'+summary(o)+'</td><td>'+money(orderRevenue(o)+o.shipping)+'</td><td>'+badge(o.status)+'</td></tr>').join("")}
function summary(o){return o.items.map(i=>(products[i.product]?.name||"منتج")+" × "+i.qty).join(" • ")}
function renderOrders(){const f=document.querySelector("#orderFilter").value;document.querySelector("#ordersTable").innerHTML=orders.slice().reverse().filter(o=>f==="all"||o.status===f).map(o=>'<tr><td class="order-id">'+o.id+'</td><td>'+o.name+'</td><td>'+summary(o)+'</td><td>'+money(orderRevenue(o)+o.shipping)+'</td><td>'+badge(o.status)+'</td><td><select class="status-change" data-id="'+o.id+'">'+Object.keys(statusNames).map(s=>'<option value="'+s+'" '+ (o.status===s?'selected':'')+'>'+statusNames[s]+'</option>').join("")+'</select></td></tr>').join("")||'<tr><td colspan="6">لا توجد طلبات بهذه الحالة</td></tr>'}
function renderProducts(){document.querySelector("#productsGrid").innerHTML=products.map((p,i)=>'<div class="product-card"><div class="product-art">'+(p.image?'<img src="'+p.image+'" alt="'+p.name+'">':p.icon)+'</div><h4>'+p.name+'</h4><p>'+p.type+'</p><strong>'+money(p.price)+'</strong><p style="margin-top:8px">المخزون: '+p.stock+'</p><button class="primary product-edit" data-index="'+i+'">تعديل المنتج</button></div>').join("")}
function renderInventory(){document.querySelector("#inventoryTable").innerHTML=products.map(p=>'<tr><td>'+(p.image?'<img class="tiny-product" src="'+p.image+'">':p.icon)+' '+p.name+'</td><td>'+p.type+'</td><td>'+p.stock+'</td><td>'+money(p.cost)+'</td><td>'+(p.stock<=1?'<span class="status-pill s-preparing">مخزون منخفض</span>':'<span class="status-pill s-delivered">متوفر</span>')+'</td></tr>').join("")}
function renderLow(){document.querySelector("#lowStockList").innerHTML=products.filter(p=>p.stock<=1).map(p=>'<div class="stock-item"><div class="mini">'+(p.image?'<img src="'+p.image+'">':p.icon)+'</div><div><b>'+p.name+'</b><small>'+p.type+'</small></div><span class="qty">'+p.stock+' متبقي</span></div>').join("")||'<div class="empty-note">المخزون جيد 🌸</div>'}
function delivered(){return orders.filter(o=>o.status==="delivered")}
function calc(){const ds=delivered(),sales=ds.reduce((s,o)=>s+orderRevenue(o),0),cost=ds.reduce((s,o)=>s+orderCost(o),0),exp=expenses.reduce((s,e)=>s+e.amount,0),profit=sales-cost-exp,inventory=products.reduce((s,p)=>s+p.stock*p.cost,0),low=products.filter(p=>p.stock<=1).length;
document.querySelector("#salesValue").innerHTML=money(sales);document.querySelector("#profitValue").innerHTML=money(profit);document.querySelector("#ordersCount").innerHTML=orders.length+' <small>طلب</small>';document.querySelector("#newOrdersCount").textContent=orders.filter(o=>o.status==="new").length+" طلب جديد";document.querySelector("#inventoryValue").innerHTML=money(inventory);document.querySelector("#lowCount").textContent=low+" منتجات مخزون منخفض";
document.querySelector("#reportSales").innerHTML=money(sales);document.querySelector("#reportCost").innerHTML=money(cost);document.querySelector("#reportProfit").innerHTML=money(sales-cost);document.querySelector("#reportDelivered").innerHTML=ds.length+' <small>طلب</small>';
}
function renderAll(){renderRecent();renderOrders();renderProducts();renderInventory();renderLow();calc()}
renderAll();
const logoutBtn=document.createElement("button");logoutBtn.className="logout-preview";logoutBtn.textContent="تسجيل خروج";logoutBtn.onclick=()=>{localStorage.removeItem(AUTH_KEY);setPreviewAuth(false)};document.querySelector(".sidebar-bottom").appendChild(logoutBtn);
document.querySelectorAll(".nav").forEach(b=>b.addEventListener("click",()=>switchPage(b.dataset.page)));
document.querySelectorAll("[data-go]").forEach(b=>b.addEventListener("click",()=>switchPage(b.dataset.go)));
function switchPage(page){document.querySelectorAll(".nav").forEach(n=>n.classList.toggle("active",n.dataset.page===page));document.querySelectorAll(".page").forEach(p=>p.classList.toggle("active",p.id==="page-"+page));const names={overview:"نظرة عامة",orders:"إدارة الطلبات",products:"المنتجات",inventory:"المخزون",expenses:"المصروفات",reports:"التقارير"};document.querySelector("#pageTitle").textContent=names[page]||"Sakura Manager"}
document.querySelector("#orderFilter").addEventListener("change",renderOrders);
document.querySelector("#ordersTable").addEventListener("change",e=>{if(!e.target.matches(".status-change"))return;const o=orders.find(x=>x.id===e.target.dataset.id);if(!o)return;const next=e.target.value,prev=o.status;if(prev===next)return;
if(next==="delivered"&&!o.stockApplied){for(const item of o.items){const p=products[item.product];if(!p||p.stock<item.qty){alert("الكمية غير كافية للمخزون لإتمام التسليم.");renderOrders();return}}for(const item of o.items)products[item.product].stock-=item.qty;o.stockApplied=true}
if(next==="cancelled"&&o.stockApplied){for(const item of o.items)products[item.product].stock+=item.qty;o.stockApplied=false}
if(prev==="cancelled"&&next!=="cancelled"&&o.stockApplied){for(const item of o.items)products[item.product].stock-=item.qty}
o.status=next;save();renderAll()});
const modal=document.querySelector("#modal");function showModal(title,body){document.querySelector("#modalTitle").textContent=title;document.querySelector("#modalBody").innerHTML=body;modal.classList.remove("hidden")}
document.querySelector("#closeModal").onclick=()=>modal.classList.add("hidden");modal.addEventListener("click",e=>{if(e.target===modal)modal.classList.add("hidden")});
document.querySelector("#addProduct").onclick=()=>showModal("إضافة منتج",'<label>اسم المنتج</label><input id="pName" placeholder="اسم المنتج"><label>القسم / المقاس</label><input id="pType" placeholder="Mini Size · 10ml أو Full Size"><label>سعر البيع</label><input id="pPrice" type="number" min="0" placeholder="السعر"><label>التكلفة</label><input id="pCost" type="number" min="0" placeholder="التكلفة"><label>الكمية</label><input id="pStock" type="number" min="0" step="0.01" placeholder="الكمية"><label>صورة المنتج</label><input id="pImage" type="file" accept="image/*"><div id="imagePreview"></div><button class="primary" id="saveProduct">حفظ المنتج</button>');
document.querySelector("#modalBody").addEventListener("change",e=>{if(e.target.id==="pImage"&&e.target.files[0]){const reader=new FileReader();reader.onload=()=>document.querySelector("#imagePreview").innerHTML='<img class="upload-preview" src="'+reader.result+'">';reader.readAsDataURL(e.target.files[0])}});
document.querySelector("#modalBody").addEventListener("click",e=>{if(e.target.id==="saveProduct"){const name=document.querySelector("#pName").value.trim(),type=document.querySelector("#pType").value.trim()||"Full Size",price=Number(document.querySelector("#pPrice").value),cost=Number(document.querySelector("#pCost").value),stock=Number(document.querySelector("#pStock").value),file=document.querySelector("#pImage").files[0];if(!name||price<0||cost<0||!Number.isFinite(stock)||stock<0){alert("راجع الاسم والسعر والتكلفة والكمية");return}const saveProduct=image=>{products.push({name,type,price,cost,stock,icon:"🧴",image});save();renderAll();modal.classList.add("hidden")};if(file){const reader=new FileReader();reader.onload=()=>saveProduct(reader.result);reader.readAsDataURL(file)}else saveProduct("")}});
document.querySelector("#productsGrid").addEventListener("click",e=>{const b=e.target.closest(".product-delete");if(!b)return;if(confirm("حذف المنتج من بيانات المعاينة؟")){products.splice(Number(b.dataset.index),1);save();renderAll()}});

document.querySelector("#productsGrid").addEventListener("click",e=>{
 const b=e.target.closest(".product-edit");if(!b)return;const idx=Number(b.dataset.index),p=products[idx];if(!p)return;
 showModal("تعديل المنتج",'<label>اسم المنتج</label><input id="editName" value="'+p.name.replace(/"/g,'&quot;')+'"><label>القسم / الحجم</label><input id="editType" value="'+p.type.replace(/"/g,'&quot;')+'"><label>سعر البيع</label><input id="editPrice" type="number" min="0" value="'+p.price+'"><label>التكلفة</label><input id="editCost" type="number" min="0" value="'+p.cost+'"><label>المخزون</label><input id="editStock" type="number" min="0" step="0.01" value="'+p.stock+'"><label>تغيير الصورة (اختياري)</label><input id="editImage" type="file" accept="image/*"><div id="editImagePreview">'+(p.image?'<img class="upload-preview" src="'+p.image+'">':'')+'</div><button class="primary" id="saveEditedProduct">حفظ التعديلات</button>');
 const body=document.querySelector("#modalBody");
 body.addEventListener("change",e=>{if(e.target.id==="editImage"&&e.target.files[0]){const r=new FileReader();r.onload=()=>document.querySelector("#editImagePreview").innerHTML='<img class="upload-preview" src="'+r.result+'">';r.readAsDataURL(e.target.files[0])}});
 body.addEventListener("click",e=>{if(e.target.id!=="saveEditedProduct")return;const name=document.querySelector("#editName").value.trim(),type=document.querySelector("#editType").value.trim(),price=Number(document.querySelector("#editPrice").value),cost=Number(document.querySelector("#editCost").value),stock=Number(document.querySelector("#editStock").value),file=document.querySelector("#editImage").files[0];if(!name||!type||price<0||cost<0||!Number.isFinite(stock)||stock<0){alert("راجع بيانات المنتج");return}const apply=image=>{p.name=name;p.type=type;p.price=price;p.cost=cost;p.stock=stock;if(image!==null)p.image=image;save();renderAll();modal.classList.add("hidden")};if(file){const r=new FileReader();r.onload=()=>apply(r.result);r.readAsDataURL(file)}else apply(null)});
});

document.querySelector("#addExpense").onclick=()=>showModal("إضافة مصروف",'<label>اسم المصروف</label><input id="expenseName" placeholder="مثال: إعلان"><label>المبلغ</label><input id="expenseAmount" type="number" min="0" placeholder="المبلغ"><label>التاريخ</label><input id="expenseDate" type="date"><button class="primary" id="saveExpense">حفظ المصروف</button>');
document.querySelector("#modalBody").addEventListener("click",e=>{if(e.target.id==="saveExpense"){const name=document.querySelector("#expenseName").value.trim(),amount=Number(document.querySelector("#expenseAmount").value),date=document.querySelector("#expenseDate").value;if(!name||amount<=0){alert("أدخل اسم المصروف ومبلغ صحيح");return}expenses.push({name,amount,date});save();renderAll();modal.classList.add("hidden")}});
