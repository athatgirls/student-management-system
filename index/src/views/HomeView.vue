<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png">
    <HelloWorld msg="Welcome to Your Vue.js App"/>
  </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from '@/components/HelloWorld.vue'

export default {
  name: 'HomeView',
  components: {
    HelloWorld
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
  text-align: center;
}

.home img {
  max-width: 100%;
  height: auto;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .home {
    padding: 15px;
  }
}

@media (max-width: 480px) {
  .home {
    padding: 10px;
  }
}
</style>
