Component({
  properties: {
    carModel: {
      type: Object,
      value: {}
    }
  },
  
  methods: {
    onTap() {
      this.triggerEvent('tap', { id: this.properties.carModel.id });
    },
    
    onCollectTap() {
      const carModel = this.properties.carModel;
      this.triggerEvent('collect', { 
        id: carModel.id,
        collected: !carModel.collected
      });
    },
    
    stopPropagation() {}
  }
});