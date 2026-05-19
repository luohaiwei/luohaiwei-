<template>
  <div class="device-entry-container">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>设备入库</h2>
        <span class="header-sub">管理实验室设备的录入、编辑与维护</span>
      </div>
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon> 入库设备
      </el-button>
    </div>

    <div class="page-card search-card">
      <div class="search-row">
        <el-input v-model="searchForm.keyword" placeholder="搜索设备名称/编号" clearable style="width: 240px" @keyup.enter="loadData" />
        <el-select v-model="searchForm.categoryId" placeholder="设备分类" clearable style="width: 160px">
          <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
        </el-select>
        <el-select v-model="searchForm.laboratory" placeholder="所属实验室" clearable style="width: 180px">
          <el-option v-for="lab in labOptions" :key="lab.value" :label="lab.label" :value="lab.value" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column label="设备图片" width="90" align="center">
          <template #default="{ row }">
            <div class="device-img-wrapper">
              <img v-if="row.imagePath" :src="resolvePublicUpload(row.imagePath)" alt="" />
              <span v-else>无图</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceNo" label="编号" min-width="120" />
        <el-table-column label="分类" min-width="120">
          <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="laboratory" label="所属实验室" min-width="130" />
        <el-table-column prop="manufacturer" label="厂商" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="info" plain @click="openView(row)">详情</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <!-- 新增入库弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      title="设备入库"
      width="680px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备编号" prop="deviceNo">
              <el-input v-model="form.deviceNo" placeholder="自动生成，可手动修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="model">
              <el-input v-model="form.model" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产厂商" prop="manufacturer">
              <el-input v-model="form.manufacturer" placeholder="必填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备价格(元)">
              <el-input-number v-model="form.price" :min="0" :precision="2" placeholder="价格" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买日期">
              <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="精度等级" prop="precisionLevel">
              <el-select v-model="form.precisionLevel" style="width: 100%">
                <el-option label="低" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="高" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在实验室" prop="laboratory">
              <el-select v-model="form.laboratory" filterable allow-create placeholder="输入或选择实验室" style="width: 100%">
                <el-option v-for="lab in labOptions" :key="lab.value" :label="lab.label" :value="lab.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细位置">
              <el-input v-model="form.location" placeholder="如：3楼301室A架" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="空闲" :value="0" />
                <el-option label="使用中" :value="1" />
                <el-option label="维修中" :value="2" />
                <el-option label="校准中" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="校准周期(天)">
              <el-input-number v-model="form.calibrationCycle" :min="30" :max="1095" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="适配实验项目">
              <el-input v-model="form.adaptProject" type="textarea" :rows="2" placeholder="可填写多个，用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备图片">
              <div class="upload-area">
                <el-upload
                  class="avatar-uploader"
                  action="#"
                  :show-file-list="false"
                  :http-request="handleImageUpload"
                  accept="image/*"
                >
                  <img v-if="imagePreview" :src="imagePreview" class="uploaded-img" />
                  <el-icon v-else class="uploader-icon"><Plus /></el-icon>
                </el-upload>
                <div class="upload-tip">
                  注意：不上传时使用默认认从后端。不支持拖拽。单张。最大2MB。建议使用横版宽屏
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备简介">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="设备简介" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleFormSubmit"
          >确认入库</el-button
        >
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="设备详情" width="600px">
      <el-descriptions v-if="viewData" :column="2" border>
        <el-descriptions-item label="设备编号">{{ viewData.deviceNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ viewData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备分类">{{ getCategoryName(viewData.categoryId) }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ viewData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生产厂商">{{ viewData.manufacturer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="购买日期">{{ viewData.purchaseDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备价格">{{ viewData.price ? '¥' + viewData.price : '-' }}</el-descriptions-item>
        <el-descriptions-item label="精度等级">{{ ['低', '中', '高'][viewData.precisionLevel - 1] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在实验室">{{ viewData.laboratory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细位置">{{ viewData.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getStatusType(viewData.status)">{{ getStatusText(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="校准周期">{{ viewData.calibrationCycle }} 天</el-descriptions-item>
        <el-descriptions-item label="适配项目" :span="2">{{ viewData.adaptProject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备简介" :span="2">{{ viewData.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getDeviceList, addDevice, updateDevice, deleteDevice, uploadDeviceImage } from '@/api/device';
import { getAllCategories } from '@/api/category';
import { getPermLabList } from '@/api/user';

const resolvePublicUpload = (path) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const s = (path || '').startsWith('/') ? path : `/${path}`;
  return `/api${s}`;
};

const tableData = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const categoryList = ref([]);
const labOptions = ref([]);
const isEdit = ref(false);
const editingId = ref(null);

const searchForm = reactive({ keyword: '', categoryId: null, laboratory: '' });
const formDialogVisible = ref(false);
const viewDialogVisible = ref(false);
const submitLoading = ref(false);
const formRef = ref();
const viewData = ref(null);

const form = reactive({
  deviceNo: '',
  deviceName: '',
  categoryId: null,
  model: '',
  manufacturer: '',
  price: null,
  purchaseDate: '',
  precisionLevel: 2,
  laboratory: '',
  location: '',
  status: 0,
  calibrationCycle: 365,
  adaptProject: '',
  imagePath: '',
  description: ''
});

const rules = {
  deviceNo: [
    { required: true, message: '请输入设备编号', trigger: 'blur' },
    { min: 2, max: 50, message: '编号长度为 2～50 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '编号仅允许字母、数字、点、下划线、连字符', trigger: 'blur' }
  ],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择设备分类', trigger: 'change' }],
  manufacturer: [{ required: true, message: '请输入生产厂商', trigger: 'blur' }],
  laboratory: [{ required: true, message: '请选择所在实验室', trigger: 'change' }]
};

const imagePreview = computed(() => {
  if (!form.imagePath) return '';
  if (form.imagePath.startsWith('http://') || form.imagePath.startsWith('https://')) return form.imagePath;
  const s = form.imagePath.startsWith('/') ? form.imagePath : `/${form.imagePath}`;
  return `/api${s}`;
});

const getStatusType = s => ({ 0: 'success', 1: 'primary', 2: 'warning', 3: 'info' })[s] || 'info';
const getStatusText = s => ({ 0: '空闲', 1: '使用中', 2: '维修中', 3: '校准中' })[s] || '未知';

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDeviceList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      deviceName: searchForm.keyword || undefined,
      categoryId: searchForm.categoryId || undefined,
      laboratory: searchForm.laboratory || undefined
    });
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const loadCategories = async () => {
  try {
    const list = await getAllCategories();
    categoryList.value = list || [];
  } catch (e) {
    console.error(e);
  }
};

const loadLabs = async () => {
  try {
    const labs = await getPermLabList();
    labOptions.value = (labs || []).map(lab => ({ value: lab.labName || lab.value, label: lab.labName || lab.label }));
  } catch (e) {
    console.error(e);
  }
};

const getCategoryName = (id) => {
  const cat = categoryList.value.find(c => c.id === id);
  return cat ? cat.categoryName : '-';
};

const openAdd = () => {
  isEdit.value = false;
  editingId.value = null;
  resetForm();
  formDialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  editingId.value = row.id;
  Object.assign(form, {
    deviceNo: row.deviceNo || '',
    deviceName: row.deviceName || '',
    categoryId: row.categoryId || null,
    model: row.model || '',
    manufacturer: row.manufacturer || '',
    price: row.price ?? null,
    purchaseDate: row.purchaseDate || '',
    precisionLevel: row.precisionLevel ?? 2,
    laboratory: row.laboratory || '',
    location: row.location || '',
    status: row.status ?? 0,
    calibrationCycle: row.calibrationCycle ?? 365,
    adaptProject: row.adaptProject || '',
    imagePath: row.imagePath || '',
    description: row.description || ''
  });
  formDialogVisible.value = true;
};

const openView = (row) => {
  viewData.value = row;
  viewDialogVisible.value = true;
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该设备吗？', '提示', { type: 'warning' });
    await deleteDevice(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败');
    }
  }
};

const resetForm = () => {
  form.deviceNo = '';
  form.deviceName = '';
  form.categoryId = null;
  form.model = '';
  form.manufacturer = '';
  form.price = null;
  form.purchaseDate = '';
  form.precisionLevel = 2;
  form.laboratory = '';
  form.location = '';
  form.status = 0;
  form.calibrationCycle = 365;
  form.adaptProject = '';
  form.imagePath = '';
  form.description = '';
};

const handleImageUpload = async ({ file }) => {
  try {
    const res = await uploadDeviceImage(file);
    if (res.path) {
      form.imagePath = res.path;
      ElMessage.success('图片上传成功');
    }
  } catch (e) {
    ElMessage.error(e.message || '图片上传失败');
  }
};

const handleFormSubmit = async () => {
  if (!formRef.value) {
    ElMessageBox.alert('表单未初始化，请刷新页面重试', '错误', { type: 'error' });
    return;
  }

  try {
    const valid = await new Promise((resolve) => {
      formRef.value.validate((isValid, invalidFields) => {
        if (!isValid) {
          if (invalidFields && Object.keys(invalidFields).length > 0) {
            const firstField = Object.keys(invalidFields)[0];
            const firstError = invalidFields[firstField];
            if (firstError && firstError.length > 0) {
              ElMessage.warning({ message: firstError[0].message || '请检查表单填写是否完整', duration: 5000 });
            }
          }
          resolve(false);
        } else {
          resolve(true);
        }
      });
    });

    if (!valid) return;

    submitLoading.value = true;

    const payload = {
      deviceNo: form.deviceNo,
      deviceName: form.deviceName,
      categoryId: form.categoryId,
      model: form.model || null,
      manufacturer: form.manufacturer,
      price: form.price != null ? Number(form.price) : null,
      purchaseDate: form.purchaseDate || null,
      precisionLevel: form.precisionLevel,
      laboratory: form.laboratory,
      location: form.location || null,
      status: form.status,
      calibrationCycle: form.calibrationCycle,
      adaptProject: form.adaptProject || null,
      imagePath: form.imagePath || null,
      description: form.description || null
    };

    if (isEdit.value) {
      await updateDevice(editingId.value, payload);
      ElMessage.success('设备更新成功');
    } else {
      await addDevice(payload);
      ElMessage.success('设备入库成功');
    }
    formDialogVisible.value = false;
    loadData();
    loadLabs();
  } catch (e) {
    let errorMsg = '操作失败，请稍后重试'
    if (e?.response?.data) {
      const data = e.response.data
      if (typeof data === 'string') {
        errorMsg = data.trim()
      } else if (typeof data === 'object' && data !== null) {
        errorMsg = data.message || data.msg || data.error || JSON.stringify(data)
      }
    } else if (e?.message && e.message !== 'Request failed with status code 400') {
      errorMsg = e.message
    }
    ElMessage.error({ message: errorMsg, duration: 5000, showClose: true })
  } finally {
    submitLoading.value = false;
  }
};

onMounted(() => {
  loadData();
  loadCategories();
  loadLabs();
});
</script>

<style scoped>
.device-entry-container {
  color: #e6edf3;
}
.page-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.page-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #161b22 0%, #1a1f35 100%);
  border-left: 4px solid #00d4ff;
  padding: 18px 24px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-header-card h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e6edf3;
}
.header-sub {
  font-size: 12px;
  color: #8b949e;
}
.search-card {
  padding-bottom: 16px;
}
.search-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}
.table-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px 24px;
}
.device-img-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #21262d;
  border: 1px solid #30363d;
  display: flex;
  align-items: center;
  justify-content: center;
}
.device-img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-area {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.upload-tip {
  font-size: 12px;
  color: #8b949e;
  line-height: 1.5;
  max-width: 260px;
}
.avatar-uploader {
  width: 120px;
  height: 120px;
  border: 1px dashed #30363d;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}
.avatar-uploader:hover {
  border-color: #00d4ff;
}
.uploader-icon {
  font-size: 28px;
  color: #8b949e;
}
.uploaded-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-header-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>