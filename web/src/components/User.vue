<template>
  <a-table :columns="columns"
           :rowKey="record => record.id"
           :dataSource="data"
           :pagination="pagination"
           :loading="loading"
           @change="handleTableChange"
  >

  </a-table>
</template>

<script>
  const columns = [{
    title: 'ID',
    dataIndex: 'id',
    sorter: true,
    width: '20%'
  }, {
    title: 'Name',
    dataIndex: 'name',
    sorter: true,
    scopedSlots: { customRender: 'name' }
  }];

  export default {
    name: "User",
    mounted() {
      this.fetch();
    },
    data() {
      return {
        data: [],
        pagination: {},
        loading: false,
        columns
      }
    },
    methods: {
      handleTableChange (pagination, filters, sorter) {
        const pager = { ...this.pagination };
        pager.current = pagination.current;
        this.pagination = pager;
        this.fetch({
          pageSize: pagination.pageSize,
          pageNum: pagination.current,
          sortField: sorter.field,
          sortOrder: sorter.order,
          ...filters
        });
      },
      fetch (ps = {}) {
        ps = {
          pageSize: 10,
          pageNum: 1,
          ...ps
        };
        this.loading = true;

        this.$api.user({params: ps})
          .then((resp) => {
            const pagination = { ...this.pagination };
            pagination.total = resp.data.total;
            this.loading = false;
            this.data = resp.data.list;
            this.pagination = pagination;
          })
          .catch((error) => {
            this.$router.push('/Login');
          });
      }
    }
  }
</script>

<style scoped>

</style>
