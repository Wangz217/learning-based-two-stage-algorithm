# -*- coding: utf-8 -*-
"""
regression task using knn, rf and lr

"""

import numpy as np
import pandas as pd
from pandas import Series
from sklearn.metrics import mean_squared_error #MSE
from sklearn.metrics import mean_absolute_error #MAE
from sklearn.metrics import r2_score
from sklearn.model_selection import train_test_split, cross_val_score
#import seaborn as sns
import matplotlib.pyplot as plt
import pickle

from sklearn.neighbors import KNeighborsRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import RandomizedSearchCV
from sklearn.model_selection import GridSearchCV
from sklearn import linear_model
from sklearn.linear_model import LinearRegression

#from sklearn import linear_model

#引入sklearn2pmml包
#from sklearn2pmml import sklearn2pmml
#from sklearn2pmml.pipeline import PMMLPipeline

#  MAPE和SMAPE
def mape(y_true, y_pred):
    return np.mean(np.abs((y_pred - y_true) / y_true)) * 100

def smape(y_true, y_pred):
    return 2.0 * np.mean(np.abs(y_pred - y_true) / (np.abs(y_pred) + np.abs(y_true))) * 100

#  调用
# mape(y_true, y_pred)
# smape(y_true, y_pred)

print('Loading data...')


#导入csv数据集
task= pd.read_csv('C:\\data\\train500.csv')
#获取特征矩阵和目标数组（标签）
task_X = task.loc[0:,'lamda1':'lamda168']
task_y1 = task.loc[0:,'ct1':'ct168'] # ct
task_y2 = task.loc[0:,'xn1':'xn56']  # x_n

# create dataset
X_train, X_test, y_train, y_test = \
    train_test_split(task_X, task_y2, test_size=0.3)


###### multi-variante regression ######
print('Starting training...')
# train model
#random forest    
print('multi-variante regression for x_n using random forest')

model1 = RandomForestRegressor(random_state = 42, max_features='auto', bootstrap=True)
grid = GridSearchCV(
        estimator=model1,
        param_grid={
            'n_estimators': list(range(10,200,10)),
            'max_depth': list(range(5,20,2)),
            'min_samples_split': [5,8, 10, 12],
            'min_samples_leaf': [5,8,10,12]
        },
        cv=5, scoring='neg_mean_squared_error', verbose=2, n_jobs=-1)
grid = grid.fit(task_X,task_y2)
print("best param is" + str(grid.best_params_))
print("best score is" + str(grid.best_score_))
# 返回网格搜索后的最优模型
rf = grid.best_estimator_ 
y_pred = rf.predict(X_test)

# eval
print('The rmse of prediction is:', mean_squared_error(y_test, y_pred) ** 0.5)
#print('The smape of prediction is:', smape(y_test, y_pred))
print('The r-square of prediction is:', r2_score(y_test, y_pred, sample_weight=None))

#knn
print('multi-variante regression for x_n using knn')

model2 = KNeighborsRegressor(algorithm='auto', weights='uniform')
grid = GridSearchCV(
        estimator=model2,
        param_grid={
                'n_neighbors': [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
                'leaf_size': list(range(10,200,5))
        },
        cv=5, scoring='neg_mean_squared_error', verbose=2, n_jobs=-1)
grid = grid.fit(task_X,task_y2)
print("best param is" + str(grid.best_params_))
print("best score is" + str(grid.best_score_))
# 返回网格搜索后的最优模型
knn = grid.best_estimator_ 
y_pred = knn.predict(X_test)

# eval
print('The rmse of prediction is:', mean_squared_error(y_test, y_pred) ** 0.5)
#print('The smape of prediction is:', smape(y_test, y_pred))
print('The r-square of prediction is:', r2_score(y_test, y_pred, sample_weight=None))


#lr
print('multi-variante regression for x_n using linear regression')

lr = linear_model.Ridge(alpha = .5)
lr.fit(X_train, y_train)
y_pred = lr.predict(X_test)

# eval
print('The rmse of prediction is:', mean_squared_error(y_test, y_pred) ** 0.5)
#print('The smape of prediction is:', smape(y_test, y_pred))
print('The r-square of prediction is:', r2_score(y_test, y_pred, sample_weight=None))

    
#model1 = RandomForestRegressor(
#        n_estimators=30,
#        max_depth=30,min_samples_split=15, 
#        min_samples_leaf=15, min_weight_fraction_leaf=0.0,
#        max_features='auto', max_leaf_nodes=500,
#        min_impurity_decrease=1e-04,bootstrap=True,
#        oob_score=False, 
#        random_state=None, verbose=0,
#        warm_start=True,
#        n_jobs=-1)
#model1.fit(X_train,y_train)
#y_pred = model1.predict(X_test)


    
##rf1 = RandomForestRegressor(random_state = 42, max_features='auto', bootstrap=True)
##random_grid = {'n_estimators': [70,100,120,150],
##               'max_depth': [8,10,12,15],
##               'min_samples_split': [2,5,8,11],
##               'min_samples_leaf': [2,5,8,11]}
##
##
##rf_random = RandomizedSearchCV(
##        estimator = rf1, param_distributions = random_grid, 
##        n_iter = 240, cv = 3, verbose=1, 
##        random_state=42, n_jobs = -1)# Fit the random search model
##rf_random.fit(X_train,y_train)
##y_pred = rf_random.predict(X_test) 
#
#rf1 = RandomForestRegressor(random_state = 42, max_features='auto', bootstrap=True)
#grid = GridSearchCV(
#        estimator=rf1,
#        param_grid={
#            'n_estimators': [10,30,50,80],
#            'max_depth': list(range(5,20,2)),
#            'min_samples_split': [5,8,10,12],
#            'min_samples_leaf': [5,8,10,12]
#        },
#        cv=3, scoring='mean_squared_error', verbose=2, n_jobs=-1)
#grid.fit(X_train,y_train)
#print(grid.best_score_)
#print (grid.best_param_)
#y_pred = grid.predict(X_test)

#print('Saving model...')
## save model to file
##with open('D:\WZX\python\RF_model.pkl', 'wb') as f:
##    pickle.dump(model1, f)
#
#print('Starting predicting...')
#
# eval
#print('The rmse of prediction is:', mean_squared_error(y_test, y_pred) ** 0.5)
##print('The smape of prediction is:', smape(y_test, y_pred))
#print('The r-square of prediction is:', r2_score(y_test, y_pred, sample_weight=None))


# predict
#data_in = [[4.71,7.34,8.24,4.48,5.41,4.48,6.22,5.53,5.08,3.7,4.77,9.09,5.63,5.18,3.9,2.82,1.54,0.89,0.75,0.94,0.76,0.69,1.58,1.89,5.84,8.39,10.09,3.56,4.01,5.32,4.16,6.17,5.29,2.94,4.05,8.45,6.7,4.14,3.56,2.93,1.75,1.4,1.11,0.56,0.68,1.71,1.06,1.71,4.34,7.43,8.38,5.12,5.09,5.06,5.44,4.4,4.93,4.8,3.82,10.44,5.9,4.29,3.25,2.98,1.26,1.36,1.34,1.01,1.05,1.02,1.14,2.3,5.06,7.01,9.53,4.74,6.9,5.36,5.23,4.7,4.13,2.82,5.69,9.68,5.81,4.46,4.44,3.78,1.11,0.6,0.75,0.6,0.84,1.09,1.03,1.75,5.22,7.27,7.49,6.48,4.45,4.88,5.02,5.23,5.61,3.61,4.46,8.77,6.46,5.04,4.08,3.85,1.46,1.61,0.77,0.74,1.09,1.44,1.06,1.57,3.88,6.67,9.44,4.94,3.82,5.12,4.33,5.28,4.08,3.97,6.5,9.06,7.45,3.85,4.14,3.52,1.72,0.93,0.82,0.71,0.81,1.1,1.12,2.0,4.45,5.35,8.68,5.4,3.67,4.3,4.23,4.76,4.64,3.6,6.29,7.98,5.2,4.49,4.84,2.97,1.43,0.95,1.12,0.82,0.59,0.96,1.64,1.7]]
#y1_pred = model1.predict(data_in)
#print(y1_pred)
# y_pred = gbm.predict(X_test, num_iteration=gbm.best_iteration)


'''
###### multi-variante classification ######
print('multi-variante reg for ct using kNN')
print('Starting training...')

# # create dataset
X_train, X_test, y_train, y_test = \
    train_test_split(task_X, task_y2, test_size=0.2)



# train model
model = KNeighborsRegressor(n_neighbors=5, radius = 1, leaf_size=50, metric = 'l2')
model.fit(X_train,y_train)


print('Saving model...')
# save model to file
#with open('D:\WZX\python\kNN_model.pkl', 'wb') as f:
#    pickle.dump(model, f)
    
print('Starting predicting...')
y_pred = model1.predict(X_test) 
#eval

print('The rmse of prediction is:', mean_squared_error(y_test, y_pred) ** 0.5)
#print('The smape of prediction is:', smape(y_test, y_pred))
print('The r-square of prediction is:', r2_score(y_test, y_pred, sample_weight=None))
# predict
#data_in = [[4.71,7.34,8.24,4.48,5.41,4.48,6.22,5.53,5.08,3.7,4.77,9.09,5.63,5.18,3.9,2.82,1.54,0.89,0.75,0.94,0.76,0.69,1.58,1.89,5.84,8.39,10.09,3.56,4.01,5.32,4.16,6.17,5.29,2.94,4.05,8.45,6.7,4.14,3.56,2.93,1.75,1.4,1.11,0.56,0.68,1.71,1.06,1.71,4.34,7.43,8.38,5.12,5.09,5.06,5.44,4.4,4.93,4.8,3.82,10.44,5.9,4.29,3.25,2.98,1.26,1.36,1.34,1.01,1.05,1.02,1.14,2.3,5.06,7.01,9.53,4.74,6.9,5.36,5.23,4.7,4.13,2.82,5.69,9.68,5.81,4.46,4.44,3.78,1.11,0.6,0.75,0.6,0.84,1.09,1.03,1.75,5.22,7.27,7.49,6.48,4.45,4.88,5.02,5.23,5.61,3.61,4.46,8.77,6.46,5.04,4.08,3.85,1.46,1.61,0.77,0.74,1.09,1.44,1.06,1.57,3.88,6.67,9.44,4.94,3.82,5.12,4.33,5.28,4.08,3.97,6.5,9.06,7.45,3.85,4.14,3.52,1.72,0.93,0.82,0.71,0.81,1.1,1.12,2.0,4.45,5.35,8.68,5.4,3.67,4.3,4.23,4.76,4.64,3.6,6.29,7.98,5.2,4.49,4.84,2.97,1.43,0.95,1.12,0.82,0.59,0.96,1.64,1.7]]
#y2_pred = model.predict(data_in)
#print(y2_pred)
'''


'''
print('Transforming model...')
#使用PMMLPipeline包裹具体评估器
pmml_pipeline  = PMMLPipeline([("RFRegressor", model1)])

#task_y2.columns = [i.split('_', 1)[0] for i in y_train.columns]
pmml_pipeline .fit(X_train,y_train)

print('Saving model...')
#保存模型到指定文件
sklearn2pmml(pmml_pipeline , "C:\\data\\RFRegressor.pmml", with_repr=True)
'''